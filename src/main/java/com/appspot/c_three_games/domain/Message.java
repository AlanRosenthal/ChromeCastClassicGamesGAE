package com.appspot.c_three_games.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.appspot.c_three_games.domain.war.Card;
import com.appspot.c_three_games.domain.war.Game;
import com.appspot.c_three_games.domain.war.Player;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gson.Gson;

public class Message {
  private static final Logger log = Logger.getLogger(Message.class.getName());

  private ChannelMessage channelMessage;

  private GCMBody gcmBody;

  public enum ChannelType {
    GAME_STARTED, PLAYER_PLAYED_CARD, PLAYER_WON_ROUND, PLAYER_LOST, PLAYER_WON, NEW_ROUND_STARTED, WAR, NEW_PLAYER
  }

  public enum PlayerType {
    GAME_STARTED, PLAYER_TURN_OVER, PLAYER_NOTINWAR, PLAYER_ALREADY_LOST, PLAYER_LOST, PLAYER_WON, GAME_NOTSTARTED,
    WAR, PLAYER_WON_ROUND
  }

  public static final class Builder {
    private Game game;
    private ArrayList<Player> players;
    private ArrayList<Card> cards;
    private ChannelType cType;
    private PlayerType pType;

    public Builder() {
      players = new ArrayList<Player>();
      cards = new ArrayList<Card>();
    }

    public Builder setGame(Game game) {
      this.game = game;
      return this;
    }

    public Builder setMessageType(ChannelType cType) {
      this.cType = cType;
      return this;
    }

    public Builder setMessageType(PlayerType pType) {
      this.pType = pType;
      return this;
    }

    public Builder addPlayer(Player player) {
      this.players.add(player);
      return this;
    }

    public Builder addPlayers(ArrayList<Player> players) {
      this.players.addAll(players);
      return this;
    }

    public Builder addCard(Card card) {
      this.cards.add(card);
      return this;
    }

    public Builder addCards(ArrayList<Card> cards) {
      this.cards.addAll(cards);
      return this;
    }

    public Message build() {
      return new Message(this);
    }
  }

  public Message(Builder builder) {
    if (builder.cType != null) {
      String message = "";
      String game = "";
      String playerIds = "";
      String playerNums = "";
      String cards = "";
      if (builder.game != null) {
        game = builder.game.getId().toString();
      }
      if (builder.players.size() > 0) {
        playerIds = builder.players.get(0).getId().toString();
        playerNums = Integer.toString(builder.players.get(0).getNum());
        for (int i = 1; i < builder.players.size(); i++) {
          playerIds += "," + builder.players.get(i).getId().toString();
          playerNums += "," + Integer.toString(builder.players.get(i).getNum());
        }
      }
      if (builder.cards.size() > 0) {
        cards = builder.cards.get(0).getName().toString();
        for (int i = 1; i < builder.cards.size(); i++) {
          cards += "," + builder.cards.get(i).getName();
        }
      }
      switch (builder.cType) {
        case GAME_STARTED:
          message = String.format("game started:%s:", game);
          break;
        case NEW_ROUND_STARTED:
          message = String.format("new round started:%s:", game);
          break;
        case PLAYER_LOST:
          message = String.format("player lost:%s:%s:%s:", game, playerIds, playerNums);
          break;
        case PLAYER_PLAYED_CARD:
          message = String.format("player played card:%s:%s:%s:%s:", game, playerIds, playerNums, cards);
          break;
        case PLAYER_WON:
          message = String.format("player won:%s:%s:%s:", game, playerIds, playerNums);
          break;
        case PLAYER_WON_ROUND:
          message = String.format("player won round:%s:%s:%s:%s:", game, playerIds, playerNums, cards);
          break;
        case WAR:
          message = String.format("war:%s:%s:%s", game, playerIds, playerNums);
          break;
        case NEW_PLAYER:
          message = String.format("new player:%s:%s:%s:", game, playerIds, playerNums);
          break;
      }
      channelMessage = new ChannelMessage(game, message);
    }
    if (builder.pType != null) {
      List<String> playerRegIds = new ArrayList<String>();
      for (Player p : builder.players) {
        playerRegIds.add(p.getRegId());
      }
      switch (builder.pType) {
        case GAME_STARTED:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "game started").build();
          break;
        case PLAYER_ALREADY_LOST:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player already lost").build();
          break;
        case PLAYER_LOST:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player lost").build();
          break;
        case PLAYER_NOTINWAR:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player notinwar").build();
          break;
        case PLAYER_TURN_OVER:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player turn over").build();
          break;
        case PLAYER_WON:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player won").build();
          break;
        case GAME_NOTSTARTED:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "game notstarted").build();
          break;
        case PLAYER_WON_ROUND:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "player won round").build();
          break;
        case WAR:
          gcmBody = new GCMBody.Builder().addRegistrationIds(playerRegIds).addData("gameId", builder.game.getId())
            .addData("message", "war").build();
          break;
        default:
          break;
      }
    }
  }

  //
  // public Message(ChannelType cType, Game game) {
  // this.cType = cType;
  // this.game = game;
  // }
  //
  // public Message(ChannelType cType, Game game, Player player) {
  // this.cType = cType;
  // this.game = game;
  // this.player = player;
  // }
  //
  // public Message(ChannelType cType, Game game, Player player, Card card) {
  // this.cType = cType;
  // this.game = game;
  // this.player = player;
  // this.card = card;
  // }
  //
  // public Message(PlayerType pType, Game game, Player player) {
  // this.pType = pType;
  // this.game = game;
  // this.player = player;
  // }

  public void send() {
    if (channelMessage != null) {
      ChannelService cs = ChannelServiceFactory.getChannelService();
      cs.sendMessage(channelMessage);
    }
    if (gcmBody != null) {
      URL url;
      HttpURLConnection urlConnection = null;
      Gson gson = new Gson();
      GCMResponse gcmResponse = new GCMResponse();
      try {
        url = new URL("https://android.googleapis.com/gcm/send");
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Authorization", "key=" + "AIzaSyB8QE8fvKcKIAghdXFSEnvBfe25mdkFhgU");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
        writer.write(gcmBody.getJson());
        writer.close();
        InputStream is = urlConnection.getInputStream();
        gcmResponse = gson.fromJson(IOUtils.toString(is), GCMResponse.class);
        // log.warning(gcmResponse.toString());
        IOUtils.closeQuietly(is);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  // public void sendMessage() {
  // if (cType != null) {
  // String message = "";
  // switch (cType) {
  // case GAME_STARTED:
  // message = String.format("game started:%s:", game.getId());
  // break;
  // case NEW_ROUND_STARTED:
  // message = String.format("new round started:%s:", game.getId());
  // break;
  // case PLAYER_LOST:
  // message = String.format("player lost:%s:%s:%s:", game.getId(), player.getId(), player.getNum());
  // break;
  // case PLAYER_PLAYED_CARD:
  // message = String.format("player played card:%s:%s:%s:%s:", game.getId(), player.getId(), player.getNum(),
  // card.getName());
  // break;
  // case PLAYER_WON:
  // message = String.format("player won:%s:%s:%s:", game.getId(), player.getId(), player.getNum());
  // break;
  // case PLAYER_WON_ROUND:
  // message = String.format("player won round:%s:%s:%s:", game.getId(), player.getId(), player.getNum());
  // break;
  // case WAR:
  // message = String.format("war:%s:", game.getId());
  // break;
  // case NEW_PLAYER:
  // message = String.format("new player:%s:%s:%s:", game.getId(), player.getId(), player.getNum());
  // break;
  // default:
  // break;
  // }
  // ChannelService cs = ChannelServiceFactory.getChannelService();
  // cs.sendMessage(new ChannelMessage(game.getId().toString(), message));
  // }
  // if (pType != null) {
  // Gson gson = new Gson();
  // GCM body = new GCM();
  // GCM response = new GCM();
  // switch (pType) {
  // case PLAYER_ALREADY_LOST:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "player already lost");
  // break;
  // case PLAYER_LOST:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "player lost");
  // break;
  // case PLAYER_NOTINWAR:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "player notinwar");
  // break;
  // case PLAYER_TURN_OVER:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "player turn over");
  // break;
  // case PLAYER_WON:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "player won");
  // break;
  // case GAME_STARTED:
  // body.registration_ids.add(player.getRegId());
  // body.data.put("gameId", game.getId());
  // body.data.put("playerId", player.getId());
  // body.data.put("message", "game started");
  // break;
  // }
  // URL url;
  // HttpURLConnection urlConnection = null;
  // try {
  // url = new URL("https://android.googleapis.com/gcm/send");
  // urlConnection = (HttpURLConnection) url.openConnection();
  // urlConnection.setRequestMethod("POST");
  // urlConnection.setRequestProperty("Authorization", "key=" + "AIzaSyB8QE8fvKcKIAghdXFSEnvBfe25mdkFhgU");
  // urlConnection.setRequestProperty("Content-Type", "application/json");
  // urlConnection.setDoOutput(true);
  // OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
  // writer.write(gson.toJson(body));
  // writer.close();
  // InputStream is = urlConnection.getInputStream();
  // response = gson.fromJson(IOUtils.toString(is), GCM.class);
  // log.warning(response.toString());
  // IOUtils.closeQuietly(is);
  // } catch (MalformedURLException e) {
  // e.printStackTrace();
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  //
  // }
  // }
}
