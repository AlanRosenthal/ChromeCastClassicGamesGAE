package com.appspot.c_three_games.servlet.war;

import static com.appspot.c_three_games.service.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.c_three_games.domain.Message;
import com.appspot.c_three_games.domain.war.Card;
import com.appspot.c_three_games.domain.war.Game;
import com.appspot.c_three_games.domain.war.Player;
import com.appspot.c_three_games.domain.war.TxResult;
import com.appspot.c_three_games.spi.war.WarInternalAPI;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Work;

public class EvaluateRound extends HttpServlet {

  private static final long serialVersionUID = -8279634084865151825L;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final Long gameId = Long.parseLong(request.getParameter("gameId"));
    final Queue queue = QueueFactory.getDefaultQueue();
    TxResult<Game> result = ofy().transact(new Work<TxResult<Game>>() {
      @Override
      public TxResult<Game> run() {
        ArrayList<Message> msgs = new ArrayList<Message>();
        Game game = WarInternalAPI.getGame(gameId);
        if (game == null) {
          return new TxResult<>(new BadRequestException("Game ID invalid"));
        }
        List<Player> players;
        switch (game.getState()) {
          case JOINING:
            return new TxResult<>(new BadRequestException("Game hasn't started yet"));
          case PLAYING:
            return new TxResult<>(new BadRequestException("Round isn't over"));
          case OVER:
            return new TxResult<>(null, msgs);
          case ROUNDOVER:
            return new TxResult<>(new BadRequestException(
                "Round has already been evaluated, start a new round"));
          case EVALUATING:
            players = WarInternalAPI.getPlayers(gameId);
            break;
          default:
            return new TxResult<>(new BadRequestException(
                "Game is in an unknown/unexpected state: " + game.getState()));
        }
        // check to make sure player state is correct
        for (int i = 0; i < players.size(); i++) {
          switch (players.get(i).getState()) {
            case JOINING:
            case PLAYING:
            case WAR1:
            case WAR2:
            case WAR3:
            case WON:
              String message =
                  "player " + players.get(i).getNum() + "/" + players.get(i).getId()
                      + " is in state: " + players.get(i).getState()
                      + ". expected PLAYED, NOTINWAR, LOST";
              return new TxResult<>(new BadRequestException(message));
            case PLAYED:
            case NOTINWAR:
            case LOST:
              break;
          }
        }
        // create an array of cards being played (null = no card played this round (not in
        // round/lost))
        ArrayList<Card> cards = new ArrayList<Card>();
        for (int i = 0; i < players.size(); i++) {
          if (players.get(i).getState() == Player.State.PLAYED) {
            // get card from end of discard deck
            List<Card> discardDeck = players.get(i).getDiscardDeck();
            Card c = discardDeck.get(discardDeck.size() - 1);
            cards.add(c);
          } else {
            cards.add(null);
          }
        }
        // find out who wins
        int highestCard = 0;
        // int winners_count = 0;
        // ArrayList<Integer> winners = new ArrayList<Integer>();
        ArrayList<Player> winners = new ArrayList<Player>();
        for (int i = 0; i < cards.size(); i++) {
          Card card = cards.get(i);
          if (card != null) {
            int rank = card.getRankInt();
            if (rank > highestCard) {
              winners.clear();
              winners.add(players.get(i));
              highestCard = rank;
            } else if (rank == highestCard) {
              winners.add(players.get(i));
            }
          }
        }
        // if there's multiple winners, check to see if they can compete in the war
        if (winners.size() > 1) {
          for (int i = 0; i < winners.size(); i++) {
            Player player = winners.get(i);
            if (player.getHandDeck().size() == 0) {
              winners.remove(i);
            }
          }
        }
        // multiple winners => war!
        if (winners.size() > 1) {
          for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.getState() == Player.State.PLAYED) {
              player.setState(Player.State.NOTINWAR);
            }
          }
          for (int i = 0; i < winners.size(); i++) {
            Player player = winners.get(i);
            player.setState(Player.State.WAR1);
          }
          game.setState(Game.State.PLAYING);
          msgs.add(new Message.Builder().setMessageType(Message.ChannelType.WAR).setGame(game)
              .addPlayers(winners).build());
          msgs.add(new Message.Builder().setMessageType(Message.PlayerType.WAR).setGame(game)
              .addPlayers(winners).build());
        }
        // 1 winner
        if (winners.size() == 1) {
          ArrayList<Card> wonCards = new ArrayList<Card>();
          for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            wonCards.addAll(player.getDiscardDeck());
            player.getDiscardDeck().clear();
          }
          Card.shuffleDeck(wonCards);
          Player winner = winners.get(0);
          winner.getHandDeck().addAll(wonCards);
          msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_WON_ROUND)
              .setGame(game).addPlayer(winner).addCards(wonCards).build());
          msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_WON_ROUND)
              .setGame(game).addPlayer(winner).addCards(wonCards).build());
          game.setState(Game.State.ROUNDOVER);
          queue.add(
              ofy().getTransaction(),
              TaskOptions.Builder.withUrl("/tasks/war/StartNewRound")
                  .param("gameId", gameId.toString()).method(TaskOptions.Method.POST)
                  .countdownMillis(wonCards.size() * 1000 + 2000 + 600));
        }
        ofy().save().entity(game).now();
        ofy().save().entities(players).now();
        return new TxResult<>(game, msgs);
      }
    });
    try {
      result.getResult();
    } catch (NotFoundException e) {
      response.sendError(404, e.getMessage());
    } catch (ForbiddenException e) {
      response.sendError(403, e.getMessage());
    } catch (ConflictException e) {
      response.sendError(409, e.getMessage());
    } catch (BadRequestException e) {
      response.sendError(400, e.getMessage());
      e.printStackTrace();
    }

  }
}
