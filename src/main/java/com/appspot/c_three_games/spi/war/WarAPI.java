package com.appspot.c_three_games.spi.war;

import static com.appspot.c_three_games.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.appspot.c_three_games.Constants;
import com.appspot.c_three_games.domain.Message;
import com.appspot.c_three_games.domain.war.Card;
import com.appspot.c_three_games.domain.war.ChannelToken;
import com.appspot.c_three_games.domain.war.Game;
import com.appspot.c_three_games.domain.war.Player;
import com.appspot.c_three_games.domain.war.TxResult;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Work;

@Api(
  name = "WarAPI",
  version = "v1",
  scopes = { Constants.EMAIL_SCOPE },
  clientIds = { Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID },
  audiences = { Constants.ANDROID_AUDIENCE },
  description = "War API")
public class WarAPI {
  @ApiMethod(name = "createGame", path = "createGame", httpMethod = HttpMethod.GET)
  public Game createGame() {
    Game game = new Game();
    game.setCreated(new Date());
    game.setState(Game.State.JOINING);
    ofy().save().entity(game).now();
    return game;
  }

  @ApiMethod(name = "getGame", path = "getGame", httpMethod = HttpMethod.GET)
  public Game getGame(@Named("gameId") Long gameId) {
    return WarInternalAPI.getGame(gameId);
  }

  @ApiMethod(name = "getPlayer", path = "getPlayer", httpMethod = HttpMethod.GET)
  public Player getPlayer(@Named("gameId") Long gameId, @Named("playerId") Long playerId) {
    return WarInternalAPI.getPlayer(gameId, playerId);
  }

  @ApiMethod(name = "getPlayers", path = "getPlayers", httpMethod = HttpMethod.GET)
  public List<Player> getPlayers(@Named("gameId") Long gameId) {
    return WarInternalAPI.getPlayers(gameId);
  }

  @ApiMethod(name = "setPlayerRegId", path = "setPlayerRegId", httpMethod = HttpMethod.POST)
  public Player setPlayerRegId(@Named("gameId") final Long gameId, @Named("playerId") final Long playerId,
    @Named("regId") final String regId) throws NotFoundException, ForbiddenException, ConflictException,
    BadRequestException {
    TxResult<Player> result = ofy().transact(new Work<TxResult<Player>>() {
      @Override
      public TxResult<Player> run() {
        Game game = WarInternalAPI.getGame(gameId);
        if (game == null) {
          return new TxResult<>(new BadRequestException("Game ID invalid"));
        }
        Player player = WarInternalAPI.getPlayer(gameId, playerId);
        if (player == null) {
          return new TxResult<>(new BadRequestException("Player ID invalid"));
        }
        player.setRegId(regId);
        ofy().save().entity(player).now();
        return new TxResult<>(player);
      }
    });
    return result.getResult();
  }

  @ApiMethod(name = "createChannelToken", path = "createChannelToken", httpMethod = HttpMethod.GET)
  public ChannelToken createChannelToken(@Named("gameId") Long gameId) {
    ChannelService channelService = ChannelServiceFactory.getChannelService();
    String token = channelService.createChannel(gameId.toString());
    ChannelToken channelToken = new ChannelToken();
    Key<Game> gameKey = Key.create(Game.class, gameId);
    channelToken.setChannelToken(token);
    channelToken.setCreated(new Date());
    channelToken.setGameId(gameKey);
    ofy().save().entity(channelToken).now();
    return channelToken;
  }

  @ApiMethod(name = "joinGame", path = "joinGame", httpMethod = HttpMethod.GET)
  public Player joinGame(@Named("gameId") final Long gameId, @Named("name") final String name)
    throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    TxResult<Player> result = ofy().transact(new Work<TxResult<Player>>() {
      @Override
      public TxResult<Player> run() {
        ArrayList<Message> msgs = new ArrayList<Message>();
        Game game = getGame(gameId);
        if (game == null) {
          return new TxResult<>(new BadRequestException("Game ID invalid"));
        }
        switch (game.getState()) {
          case EVALUATING:
          case OVER:
          case PLAYING:
          case ROUNDOVER:
            return new TxResult<>(
              new BadRequestException("Game is in state: " + game.getState() + ", expected JOINING"));
          case JOINING:
            break;
        }
        int players = game.getPlayers();
        if (players == 4) {
          return new TxResult<>(new BadRequestException("4 players already"));
        }
        Key<Game> gameKey = Key.create(Game.class, gameId);
        Player player = new Player();
        player.setGameId(gameKey);
        player.setCreated(new Date());
        player.setName(name);
        player.setNum(players);
        player.setState(Player.State.JOINING);
        game.setPlayers(++players);
        ofy().save().entity(game).now();
        ofy().save().entity(player).now();
        // msgs.add(new Message(game, player, Message.messageType.NEW_PLAYER));
        // msgs.add(new Message(Message.Type.NEW_PLAYER, Message.Recipient.CHANNEL, game, player));
        msgs.add(new Message.Builder().setMessageType(Message.ChannelType.NEW_PLAYER).setGame(game).addPlayer(player)
          .build());
        return new TxResult<>(player, msgs);
      }
    });
    return result.getResult();
  }

  @ApiMethod(name = "startGame", path = "startGame", httpMethod = HttpMethod.GET)
  public Game startGame(@Named("gameId") final Long gameId) throws NotFoundException, ForbiddenException,
    ConflictException, BadRequestException {
    TxResult<Game> result = ofy().transact(new Work<TxResult<Game>>() {
      @Override
      public TxResult<Game> run() {
        ArrayList<Message> msgs = new ArrayList<Message>();
        Game game = getGame(gameId);
        if (game == null) {
          return new TxResult<>(new BadRequestException("Game ID invalid"));
        }
        switch (game.getState()) {
          case EVALUATING:
          case OVER:
          case PLAYING:
          case ROUNDOVER:
            return new TxResult<>(
              new BadRequestException("Game is in state: " + game.getState() + ", expected JOINING"));
          case JOINING:
            break;
        }
        List<Player> players = getPlayers(gameId);
        game.setStarted(new Date());
        game.setPlayers(players.size());
        game.setState(Game.State.PLAYING);
        List<Card> deck = Card.shuffleDeck(Card.fullDeck());
        for (int i = 0; i < deck.size(); i++) {
          players.get(i % players.size()).getHandDeck().add(deck.get(i));
        }

        for (int i = 0; i < players.size(); i++) {
          players.get(i).setState(Player.State.PLAYING);
        }
        // msgs.add(new Message(game, Message.messageType.GAME_STARTED));
        // msgs.add(new Message(Message.Type.GAME_STARTED, Message.Recipient.CHANNEL, game));

        msgs.add(new Message.Builder().setMessageType(Message.ChannelType.GAME_STARTED).setGame(game).build());
        // msgs.add(new Message(Message.channelType.GAME_STARTED, game));
        for (int i = 0; i < players.size(); i++) {
          msgs.add(new Message.Builder().setMessageType(Message.PlayerType.GAME_STARTED).setGame(game)
            .addPlayer(players.get(i)).build());
          // msgs.add(new Message(Message.playerType.GAME_STARTED, game, players.get(i)));
          // msgs.add(new Message(Message.Type.GAME_STARTED, Message.Recipient.PLAYER, game, players.get(i)));
        }
        ofy().save().entity(game).now();
        ofy().save().entities(players).now();
        return new TxResult<>(game, msgs);
      }
    });
    return result.getResult();
  }

  @ApiMethod(name = "playCard", path = "playCard", httpMethod = HttpMethod.POST)
  public Player playCard(@Named("gameId") final Long gameId, @Named("playerId") final Long playerId)
    throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    final Queue queue = QueueFactory.getDefaultQueue();
    TxResult<Player> result = ofy().transact(new Work<TxResult<Player>>() {
      @Override
      public TxResult<Player> run() {
        ArrayList<Message> msgs = new ArrayList<Message>();
        Game game = getGame(gameId);
        if (game == null) {
          return new TxResult<>(new BadRequestException("Game ID invalid"));
        }
        Player p1 = getPlayer(gameId, playerId);
        if (p1 == null) {
          return new TxResult<>(new BadRequestException("Player ID invalid"));
        }
        List<Player> players = getPlayers(gameId);
        Player player = players.get(p1.getNum());
        switch (game.getState()) {
          case EVALUATING:
          case ROUNDOVER:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_TURN_OVER).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(null, msgs);
          case JOINING:
            return new TxResult<>(new BadRequestException("Game hasn't started yet"));
          case OVER:
            return new TxResult<>(new BadRequestException("Game is over"));
          case PLAYING:
            break;
        }
        switch (player.getState()) {
          case PLAYED:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_TURN_OVER).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(players.get(p1.getNum()), msgs);
          case NOTINWAR:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_NOTINWAR).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(players.get(p1.getNum()), msgs);
          case LOST:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_ALREADY_LOST).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(players.get(p1.getNum()), msgs);
          case JOINING:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.GAME_NOTSTARTED).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(players.get(p1.getNum()), msgs);
          case WON:
            msgs.add(new Message.Builder().setMessageType(Message.PlayerType.PLAYER_WON).setGame(game)
              .addPlayer(player).build());
            return new TxResult<>(players.get(p1.getNum()), msgs);
          case PLAYING: {
            Card card = player.getHandDeck().remove(0);
            player.getDiscardDeck().add(card);
            msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_PLAYED_CARD).setGame(game)
              .addPlayer(player).addCard(card).build());
            player.setState(Player.State.PLAYED);
            break;
          }
          case WAR1: {
            Card card = player.getHandDeck().remove(0);
            player.getDiscardDeck().add(card);
            if (player.getHandDeck().size() == 0) {
              player.setState(Player.State.PLAYED);
            } else {
              player.setState(Player.State.WAR2);
            }
            msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_PLAYED_CARD).setGame(game)
              .addPlayer(player).addCard(card).build());
            break;
          }
          case WAR2: {
            Card card = player.getHandDeck().remove(0);
            player.getDiscardDeck().add(card);
            if (player.getHandDeck().size() == 0) {
              player.setState(Player.State.PLAYED);
            } else {
              player.setState(Player.State.WAR3);
            }
            msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_PLAYED_CARD).setGame(game)
              .addPlayer(player).addCard(card).build());
            break;
          }
          case WAR3: {
            Card card = player.getHandDeck().remove(0);
            player.getDiscardDeck().add(card);
            if (player.getHandDeck().size() == 0) {
              player.setState(Player.State.PLAYED);
            } else {
              player.setState(Player.State.PLAYING);
            }
            msgs.add(new Message.Builder().setMessageType(Message.ChannelType.PLAYER_PLAYED_CARD).setGame(game)
              .addPlayer(player).addCard(card).build());
            break;
          }
        }
        // check to see if everyone's turn is over
        int waitingTurn = 0;
        for (Player p : players) {
          switch (p.getState()) {
            case PLAYED:
            case NOTINWAR:
            case LOST:
              waitingTurn++;
              break;
            case JOINING:
            case PLAYING:
            case WAR1:
            case WAR2:
            case WAR3:
            case WON:
              break;
          }
        }
        // decide who gets the cards
        if (waitingTurn == players.size()) {
          game.setState(Game.State.EVALUATING);
          // run evaluateRound
          queue.add(
            ofy().getTransaction(),
            TaskOptions.Builder.withUrl("/tasks/EvaluateRound").param("gameId", gameId.toString())
              .method(TaskOptions.Method.POST).countdownMillis(1000));
        }

        // save
        ofy().save().entity(game).now();
        ofy().save().entities(players).now();
        return new TxResult<>(player, msgs);
      }
    });
    return result.getResult();
  }
}
