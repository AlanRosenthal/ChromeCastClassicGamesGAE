package com.appspot.c_three_games.servlet.war;

import static com.appspot.c_three_games.service.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appspot.c_three_games.domain.war.Game;
import com.appspot.c_three_games.domain.war.Message;
import com.appspot.c_three_games.domain.war.Player;
import com.appspot.c_three_games.domain.war.TxResult;
import com.appspot.c_three_games.spi.war.WarInternalAPI;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Work;

public class StartNewRound extends HttpServlet {

  private static final long serialVersionUID = -6301940623351682898L;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    final Long gameId = Long.parseLong(request.getParameter("gameId"));
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
            // return new TxResult<>(new BadRequestException("Game is over"));
          case EVALUATING:
            return new TxResult<>(new BadRequestException("Round needs to be evaluated"));
          case ROUNDOVER:
            players = WarInternalAPI.getPlayers(gameId);
            break;
          default:
            return new TxResult<>(new BadRequestException("Game is in an unknown/unexpected state: " + game.getState()));
        }
        for (int i = 0; i < players.size(); i++) {
          switch (players.get(i).getState()) {
            case JOINING:
            case PLAYING:
            case WAR1:
            case WAR2:
            case WAR3:
            case WON:
              String message = "player " + players.get(i).getNum() + "/" + players.get(i).getId() + " is in state: "
                + players.get(i).getState() + ". expected PLAYED, NOTINWAR or LOST";
              return new TxResult<>(new BadRequestException(message));
            case PLAYED:
            case NOTINWAR:
            case LOST:
              break;
          }
        }
        for (int i = 0; i < players.size(); i++) {
          Player player = players.get(i);
          // player out of cards
          if (player.getHandDeck().size() == 0) {
            if (player.getState() != Player.State.LOST) {
              player.setState(Player.State.LOST);
              msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_LOST).setGame(game)
                .addPlayer(player).build());
              msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_LOST).setGame(game)
                .addPlayer(player).build());
            }
          } else if (player.getHandDeck().size() == 52) {
            // player won
            player.setState(Player.State.WON);
            game.setState(Game.State.OVER);
            game.setEnded(new Date());
            msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_WON).setGame(game)
              .addPlayer(player).build());
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_WON).setGame(game)
              .addPlayer(player).build());
          } else {
            // new round
            player.setState(Player.State.PLAYING);
          }
        }
        if (game.getState() != Game.State.OVER) {
          game.setState(Game.State.PLAYING);
          msgs.add(new Message.Builder().setMessageType(Message.ChannelType.NEW_ROUND_STARTED).setGame(game).build());
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
