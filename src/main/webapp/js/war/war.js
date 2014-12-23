var positions = [{}, {}, {
  // two players
  'handX': [0, 1062],
  'handY': [0, 340],
  'discardX': [531, 531],
  'discardY': [90, 250],
  'discardCenterX': [531, 531],
  'discardCenterY': [90, 250],
  'warX': [-50, 50],
  'warY': [-15, 15],
  'playerX': [0, 590],
  'playerY': [0, 535],
  'playerAlign': ['left', 'right'],
  'playerLostX': [0, 730],
  'playerLostY': [35, 385]
}, {
  // three players
  'handX': [0, 1062, 1062],
  'handY': [0, 0, 340],
  'discardX': [472, 590, 531],
  'discardY': [90, 90, 250],
  'discardCenterX': 531,
  'discardCenterY': 170,
  'warX': [-50, 50, 50],
  'warY': [-15, -15, 15],
  'playerX': [0, 590, 590],
  'playerY': [0, 0, 535],
  'playerAlign': ['left', 'right', 'right'],
  'playerLostX': [0, 730, 730],
  'playerLostY': [35, 35, 385]
}, {
  // four players
  'handX': [0, 1062, 0, 1062],
  'handY': [0, 0, 340, 340],
  'discardX': [472, 590, 472, 590],
  'discardY': [90, 90, 250, 250],
  'discardCenterX': 531,
  'discardCenterY': 170,
  'warX': [-50, 50, -50, 50],
  'warY': [-15, -15, 15, 15],
  'playerX': [0, 590, 0, 590],
  'playerY': [0, 0, 535, 535],
  'playerAlign': ['left', 'right', 'left', 'right'],
  'playerLostX': [0, 730, 0, 730],
  'playerLostY': [35, 35, 385, 385]
}];

var card_z_index = 11;
var animate_time = 'slow';
var auto = false;

function initGame(gameId) {
  game.id = gameId;
  apiCreateChannel(function() {
    updateGame();
  });
}

function updateGame() {
  apiGetGame(function() {
    switch (game.state) {
    case "JOINING":
      $('.game_joining').removeClass('hidden');
      $('.game_playing').addClass('hidden');
      $('.title').text("Scan QR Code to Join!");
      $('.qrcode').append(
              '<img src="https://api.qrserver.com/v1/create-qr-code/?size=520x520&data='
                      + game.id + '">');
      apiGetPlayers(function() {
        players.forEach(function(p) {
          switch (p.state) {
          case "JOINING":
            addPlayer(p.num);
            break;
          case "PLAYING":
          case "PLAYED":
          case "WAR1":
          case "WAR2":
          case "WAR3":
          case "NOTINWAR":
          case "LOST":
          case "WON":
            console.error("invalid state");
            console.error("game: " + game.state);
            console.error("player: " + p.state);
            break;
          }
        });
      });
      break;
    case "PLAYING":
    case "EVALUATING":
    case "ROUNDOVER":
      $('.game_joining').addClass('hidden');
      $('.game_playing').removeClass('hidden');
      apiGetPlayers(function() {
        players.forEach(function(p) {
          switch (p.state) {
          case "JOINING":
          case "WON":
            console.error("invalid state");
            console.error("game: " + game.state);
            console.error("player: " + p.state);
            break;
          case "PLAYING":
          case "PLAYED":
          case "WAR1":
          case "WAR2":
          case "WAR3":
          case "NOTINWAR":
            addPlayer(p.num);
            p.handDeck.forEach(function(c) {
              createCard(c, p.num);
            });
            var discardDeck = p.discardDeck;
            p.discardDeck = [];
            discardDeck.forEach(function(c, i) {
              createCard(c, p.num);
              p.handDeck.push(c);
              playCard(c, p.num, false);
            });
            break;
          case "LOST":
            addPlayer(p.num);
            playerLost(p.num, false);
            break;
          }
        });
      });
      break;
    case "OVER":
      apiGetPlayers(function() {
        players.forEach(function(p) {
          switch (p.state) {
          case "JOINING":
          case "PLAYING":
          case "PLAYED":
          case "WAR1":
          case "WAR2":
          case "WAR3":
          case "NOTINWAR":
            console.error("invalid state");
            console.error("game: " + game.state);
            console.error("player: " + p.state);
            break;
          case "WON":
            addPlayer(p.num);
            playerWon(p.num);
            break;
          case "LOST":
            addPlayer(p.num);
            playerLost(p.num);
            break;
          }
        });
      });
      console.log("game is over!");
      break;
    }
  });
}

function newPlayer(playerId, playerNum) {
  apiGetPlayer(playerId, function() {
    addPlayer(playerNum);
  });
}

function startGame() {
  console.log("Game Started!");
  $('.title').text("Starting Game...");
  updateGame();
}

function playerWonRound(playerNum) {
  console.log("player won round: " + playerNum);
  $('.title').text(players[playerNum].name + " won round!");
  takeCards(playerNum, true);
}

function playerLost(playerNum, sound) {
  var e = $('<img id="player_' + playerNum + '" src="./img/lost.png" />');
  var top = positions[game.players].playerLostY[parseInt(playerNum)];
  var left = positions[game.players].playerLostX[parseInt(playerNum)];
  e.css({
    'top': top + 'px',
    'left': left + 'px'
  });
  e.addClass('player_lost');
  $('.game_playing').append(e);
  apiGetPlayer(players[playerNum].id);
  console.log("player " + playerNum + " lost");
  if (sound) {
    // play sound
  }
}

function playerWon(playerNum) {
  console.log("player " + playerNum + " won");
}

function playerWar() {
  $('.title').text("War!");
  console.log("war");
}

function newRound() {
  console.log("new round");
}

function addPlayer(playerNum) {
  if (game.state == "JOINING") {
    var e = $('#joining_player_' + playerNum);
    e.text(players[playerNum].name);
    e.addClass('player_name_closed');
    e.removeClass('player_name_open');
  }
  if (game.state == "PLAYING") {
    // set player name
    var e = $('.player_name#player_' + playerNum);
    var left = positions[game.players].playerX[parseInt(playerNum)];
    var top = positions[game.players].playerY[parseInt(playerNum)];
    e.text(players[playerNum].name);
    e.css({
      'top': top + 'px',
      'left': left + 'px',
      'text-align': positions[game.players].playerAlign[parseInt(playerNum)]
    });
    // log
    console.log("Player " + playerNum + ", " + players[playerNum].state);
    console.log(players[playerNum].handDeck);
  }
}

function createCard(cardName, playerNum) {
  var left = positions[game.players].handX[parseInt(playerNum)];
  var top = positions[game.players].handY[parseInt(playerNum)];
  var card = $('#' + cardName);
  card.css({
    'top': top + 'px',
    'left': left + 'px'
  });
  card.removeClass('hidden');
}

function calcDiscardX(playerNum) {
  var left = positions[game.players].discardX[parseInt(playerNum)];
  switch (game.players) {
  case 3:
    switch (parseInt(playerNum)) {
    case 0:
    case 1:
      if ((players[0].state == "LOST") || (players[1].state == "LOST")) {
        left = positions[game.players].discardCenterX;
      }
      break;
    case 2:
      break;
    }
    break;
  case 4:
    switch (parseInt(playerNum)) {
    case 0:
    case 1:
      if ((players[0].state == "LOST") || (players[1].state == "LOST")) {
        left = positions[game.players].discardCenterX;
      }
      break;
    case 2:
    case 3:
      if ((players[2].state == "LOST") || (players[3].state == "LOST")) {
        left = positions[game.players].discardCenterX;
      }
      break;
    }
    break;
  }
  return left;
}

function calcDiscardY(playerNum) {
  var top = positions[game.players].discardY[parseInt(playerNum)];
  switch (game.players) {
  case 3:
    switch (parseInt(playerNum)) {
    case 0:
    case 1:
      if (players[2].state == "LOST") {
        top = positions[game.players].discardCenterY;
      }
      break;
    case 2:
      break;
    }
    break;
  case 4:
    switch (parseInt(playerNum)) {
    case 0:
    case 1:
      if ((players[2].state == "LOST") && (players[3].state == "LOST")) {
        top = positions[game.players].discardCenterY;
      }
      break;
    case 2:
    case 3:
      if ((players[0].state == "LOST") & (players[1].state == "LOST")) {
        top = positions[game.players].discardCenterY;
      }
      break;
    }
    break;
  }
  return top;
}

function moveCard(cardName, playerNum, warLevel, animate, fn) {
  var card = $('#' + cardName);
  var left = calcDiscardX(playerNum);
  var top = calcDiscardY(playerNum);
  left += warLevel * positions[game.players].warX[parseInt(playerNum)];
  top += warLevel * positions[game.players].warY[parseInt(playerNum)];
  if (animate) {
    card.animate({
      'top': top + 'px',
      'left': left + 'px'
    }, animate_time, function() {
      if (fn !== undefined) {
        fn();
      }
    });
  } else {
    card.css({
      'top': top + 'px',
      'left': left + 'px'
    });
    if (fn !== undefined) {
      fn();
    }
  }
}

function playCard(cardName, playerNum, animate) {
  if (players[playerNum].handDeck.indexOf(cardName) > -1) {
    console.log("player: " + playerNum + ", card: " + cardName);
    var index = players[playerNum].handDeck.indexOf(cardName);
    players[playerNum].handDeck.splice(index, 1);
    players[playerNum].discardDeck.push(cardName);

    var warLevel = (players[playerNum].discardDeck.length - 2) % 4 + 1;
    if (warLevel == 1) {
      collapseCards(playerNum, animate);
    }
    flipcard(cardName, '', 'top');
    moveCard(cardName, playerNum, warLevel, animate, function() {
      // if (isLastCard == true) {
      if (players[playerNum].handDeck.length == 0) {
        flipcard(cardName, 'show', '');
      } else {
        if ((warLevel % 4) == 0) {
          flipcard(cardName, 'show', '');
        } else {
          flipcard(cardName, 'hide', '');
        }
      }
    });
  } else {
    console.error("card not in player's hand:" + playerNum + ", " + cardName);
  }
}

function collapseCards(playerNum, animate) {
  var deck = players[playerNum].discardDeck.slice();
  deck.pop();
  deck.forEach(function(cardName) {
    var card = $('#' + cardName);
    var left = calcDiscardX(playerNum);
    var top = calcDiscardY(playerNum);
    // var left = positions[game.players].discardX[parseInt(playerNum)];
    // var top = positions[game.players].discardY[parseInt(playerNum)];
    if (animate) {
      card.animate({
        'top': top + 'px',
        'left': left + 'px'
      }, animate_time);
    } else {
      card.css({
        'top': top + 'px',
        'left': left + 'px'
      });
    }
  });
}

function takeCards(playerNum, animate) {
  var cards = {
    'name': [],
    'warLevel': [],
    'player': []
  };
  // add discardDeck to cards for each player
  players.forEach(function(p) {
    cards.name = cards.name.concat(p.discardDeck);
    cards.warLevel = cards.warLevel.concat(p.discardDeck.map(function(x, i) {
      return ((i - 1) % 4) + 1;
    }));
    cards.player = cards.player.concat(p.discardDeck.map(function() {
      return p.num;
    }));
    p.discardDeck = [];
  });
  // show all cards
  setTimeout(function() {
    cards.name.forEach(function(c) {
      flipcard(c, 'show', 'top');
    });
    var handDeck = players[playerNum].handDeck.slice();
    handDeck.reverse().forEach(function(c) {
      flipcard(c, '', 'top');
    });
  }, 1000);
  var takeCardsHelperFn = function(playerNum, cards, animate) {
    if (cards.name.length > 0) {
      var warLevelRev = cards.warLevel.slice().reverse();
      var nameRev = cards.name.slice().reverse();
      var playerRev = cards.player.slice().reverse();
      if (warLevelRev[0] == 4) {
        moveCard(nameRev[0], playerRev[0], 4, animate);
        moveCard(nameRev[1], playerRev[1], 3, animate);
        moveCard(nameRev[2], playerRev[2], 2, animate);
        moveCard(nameRev[3], playerRev[3], 1, animate);
      }
      setTimeout(function() {
        var c = cards.name.pop();
        cards.warLevel.pop();
        cards.player.pop();
        flipcard(c, 'hide', '');
        var left = positions[game.players].handX[parseInt(playerNum)];
        var top = positions[game.players].handY[parseInt(playerNum)];
        if (animate) {
          $('#' + c).animate({
            'top': top + 'px',
            'left': left + 'px',
          }, animate_time);
        } else {
          $('#' + c).css({
            'top': top + 'px',
            'left': left + 'px',
          });
        }
        players[playerNum].handDeck.push(c);
        takeCardsHelperFn(playerNum, cards, animate);
      }, 1000);
    }
  };
  setTimeout(function() {
    takeCardsHelperFn(playerNum, cards, animate);
  }, 2000);

}

function flipcard(card, action, position) {
  e = $('#' + card);
  switch (position) {
  case 'top':
    e.css('z-index', card_z_index++);
    break;
  case 'bottom':
    e.css('z-index', 9);
    break;
  }
  switch (action) {
  case 'show':
    e.removeClass('facedown').addClass('faceup');
    break;
  case 'hide':
    e.removeClass('faceup').addClass('facedown');
    break;
  case 'flip':
    e.toggleClass('facedown').toggleClass('faceup');
    break;
  }
}

function channelMessage(command, args) {
  switch (command) {
  case "game started":
    // var gameId = args[0];
    startGame();
    break;
  case "new round started":
    // var gameId = args[0];
    newRound();
    players.forEach(function(p) {
      console.log(p.num + ": " + p.state + ", " + p.handDeck.length);
    });
    if (auto) {
      players.forEach(function(p) {
        setTimeout(function() {
          apiPlayCard(p.id);
        }, 200 + 200 * p.num);
      });
    }
    break;
  case "war":
    // var gameId = args[0];
    playerWar();
    if (auto) {
      players.forEach(function(p) {
        for ( var i = 0; i < 4; i++) {
          setTimeout(function() {
            apiPlayCard(p.id);
          }, 500 * i + p.num * 200);
        }
      });
    }

    break;
  case "player won round":
    // var gameId = args[0];
    // var playerId = args[1];
    var playerNum = args[2];
    playerWonRound(playerNum);
    break;
  case "player lost":
    // var gameId = args[0];
    // var playerId = args[1];
    var playerNum = args[2];
    playerLost(playerNum, true);
    break;
  case "player won":
    // var gameId = args[0];
    // var playerId = args[1];
    var playerNum = args[2];
    playerWon(playerNum);
    break;
  case "new player":
    // var gameId = args[0];
    var playerId = args[1];
    var playerNum = args[2];
    newPlayer(playerId, playerNum);
    break;
  case "player played card":
    // case "war card 1":
    // case "war card 2":
    // case "war card 3":
    // var gameId = args[0];
    // var playerId = args[1];
    var playerNum = args[2];
    var card = args[3];
    playCard(card, playerNum, true);
    // playCard(card, playerNum, false, true);
    break;
  // case "war card 1 last":
  // case "war card 2 last":
  // case "war card 3 last":
  // var gameId = args[0];
  // var playerId = args[1];
  // var playerNum = args[2];
  // var card = args[3];
  // playCard(card, playerNum, true, true);
  // break;
  default:
    console.log("no command match: " + command);
    console.log(args);
    break;
  }
}
