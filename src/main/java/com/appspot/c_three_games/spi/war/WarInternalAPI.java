package com.appspot.c_three_games.spi.war;

import static com.appspot.c_three_games.service.OfyService.ofy;

import java.util.List;

import com.appspot.c_three_games.domain.war.Game;
import com.appspot.c_three_games.domain.war.Player;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.googlecode.objectify.Key;

public class WarInternalAPI {

    public static Game getGame(Long gameId) {
        Game game = ofy().consistency(Consistency.STRONG).load().type(Game.class).id(gameId).now();
        return game;
    }

    public static Player getPlayer(Long gameId, Long playerId) {
        Key<Game> gameKey = Key.create(Game.class, gameId);
        Key<Player> playerKey = Key.create(gameKey, Player.class, playerId);
        Player player = ofy().consistency(Consistency.STRONG).load().key(playerKey).now();
        return player;
    }

    public static List<Player> getPlayers(Long gameId) {
        Key<Game> gameKey = Key.create(Game.class, gameId);
        List<Player> players = ofy().consistency(Consistency.STRONG).load().type(Player.class)
            .ancestor(gameKey).order("created").list();
        return players;
    }
}
