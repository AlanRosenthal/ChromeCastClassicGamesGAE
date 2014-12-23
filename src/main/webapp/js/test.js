$(document).keypress(function(e) {
  switch (e.keyCode) {
  case 49:
    apiPlayCard(players[0].id);
    break;
  case 50:
    apiPlayCard(players[1].id);
    break;
  case 51:
    apiPlayCard(players[2].id);
    break;
  case 52:
    apiPlayCard(players[3].id);
    break;
  default:
    break;
  }
});
