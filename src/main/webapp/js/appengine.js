var apiLoaded = false;
var game = {};
var players = [];

function init() {
  console.log("init()");
  gapi.client.load('gameApi', 'v1', function() {
    console.log("api loaded");
    apiLoaded = true;
  }, '//' + window.location.host + '/_ah/api');
}

function apiCreateGame(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.createGame().execute(function(resp) {
      if (resp.error) {
        console.log("error with getGame api");
        console.log(resp);
      } else {
        game = resp;
        console.log("Game ID: " + resp.id);
        console.log("Game State: " + resp.state);
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiJoinGame(name, fn) {
  if (apiLoaded) {
    gapi.client.gameApi.joinGame({
      'gameId': game.id,
      'name': name
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getGame api");
        console.log(resp);
      } else {
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiStartGame(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.startGame({
      'gameId': game.id,
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getGame api");
        console.log(resp);
      } else {
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiCreateChannel(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.createChannelToken({
      'gameId': game.id
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with createChannelToken api");
        console.log(resp);
      } else {
        console.log("Channel Token: " + resp.channelToken);
        createChannel(resp.channelToken);
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiGetGame(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.getGame({
      'gameId': game.id
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getGame api");
        console.log(resp);
      } else {
        game = resp;
        console.log("Game ID: " + resp.id);
        console.log("Game State: " + resp.state);
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiGetPlayer(playerId, fn) {
  if (apiLoaded) {
    gapi.client.gameApi.getPlayer({
      'gameId': game.id,
      'playerId': playerId
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getPlayer api");
        console.log(resp);
      } else {
        players[resp.num] = resp;
        sanitizePlayerCard(resp.num);
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiGetPlayers(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.getPlayers({
      'gameId': game.id
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with showPlayers api");
        console.log(resp);
      } else {
        if (resp.hasOwnProperty('items')) {
          players = resp.items;
          players.forEach(function(p) {
            sanitizePlayerCard(p.num);
          });
        }
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function sanitizePlayerCard(playerNum) {
  // change handDeck property to list of card names
  if (players[playerNum].hasOwnProperty('handDeck')) {
    players[playerNum].handDeck = players[playerNum].handDeck
            .map(function(obj) {
              return obj.name;
            });
  } else {
    players[playerNum].handDeck = [];
  }
  // change discardDeck to list of card names
  if (players[playerNum].hasOwnProperty('discardDeck')) {
    players[playerNum].discardDeck = players[playerNum].discardDeck
            .map(function(obj) {
              return obj.name;
            });
  } else {
    players[playerNum].discardDeck = [];
  }
}

function apiPlayCard(playerId, fn) {
  if (apiLoaded) {
    gapi.client.gameApi.playCard({
      'gameId': game.id,
      'playerId': playerId
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getPlayer api");
        console.log(resp);
      } else {
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiStartNewRound(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.startNewRound({
      'gameId': game.id,
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getPlayer api");
        console.log(resp);
      } else {
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}

function apiEvaluateRound(fn) {
  if (apiLoaded) {
    gapi.client.gameApi.evaluateRound({
      'gameId': game.id,
    }).execute(function(resp) {
      if (resp.error) {
        console.log("error with getPlayer api");
        console.log(resp);
      } else {
        if (fn !== undefined) {
          fn();
        }
      }
    });
  } else {
    console.error("API not loaded");
  }
}
