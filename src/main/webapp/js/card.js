var canvas;
var ctx;
var cards = [];

window.onload = function() {
	console.log("Loaded test.js");
	canvas = document.getElementById("canvas");
	ctx = canvas.getContext("2d");
	time = Date.now();
	reset();
	// main();
};
function reset() {
	var i = 0;
	game.player.forEach(function(p, player_id) {
		p.card_dealer.forEach(function(c) {
			o = {};
			o['card'] = c;
			o['x'] = 531;
			o['y'] = 175;
			o['front_img'] = new Image();
			o['front_img'].src = '../img/cards/' + c + '.png';
			o['back_img'] = new Image();
			o['back_img'].src = '../img/cards/Back-Blue.png';
			o['face'] = 'back';
			o['move'] = {};
			o['move']['moving'] = true;
			o['move']['delay'] = 1000 + 250 * i;
			o['move']['time'] = 250;
			switch (player_id) {
			case 0:
				o['move']['new_x'] = 0;
				o['move']['new_y'] = 0;
				break;
			case 1:
				o['move']['new_x'] = 1062;
				o['move']['new_y'] = 0;
				break;
			case 2:
				o['move']['new_x'] = 1062;
				o['move']['new_y'] = 350;
				break;
			case 3:
				o['move']['new_x'] = 0;
				o['move']['new_y'] = 350;
				break;
			}
			cards.push(o);
			i++;
		});
	});
}

function main() {
	var delta = Date.now() - time;
	update(delta);
	render();
	time = Date.now();
	window.requestAnimationFrame(main);
}
function update(delta) {
	cards.forEach(function(c) {
		if (c['move'].moving) {
			c['move'].delay -= delta;
			if (c['move'].delay < 0) {
				c['move'].delay = 0;
			}
			if (c['move'].delay == 0) {
				c.x -= (c.x - c['move'].new_x) * (delta / c['move'].time);
				c.y -= (c.y - c['move'].new_y) * (delta / c['move'].time);
				c['move'].time -= delta;
				if (c['move'].time < 0) {
					c['move'].moving = false;
					c.x = c['move'].new_x;
					c.y = c['move'].new_y;
				}
			}
		}
	});
	// for ( var c in cards) {
	// if (cards.hasOwnProperty(c)) {
	// if (cards[c]['move'].moving) {
	// cards[c].move.delay = cards[c].move.delay - delta;
	// if (cards[c].move.delay < 0) {
	// cards[c].move.delay = 0;
	// }
	// if (cards[c].move.delay == 0) {
	// cards[c].x = cards[c].x
	// - (cards[c].x - cards[c].move.new_x)
	// * (delta / cards[c].move.time);
	// cards[c].y = cards[c].y
	// - (cards[c].y - cards[c].move.new_y)
	// * (delta / cards[c].move.time);
	// cards[c].move.time = cards[c].move.time - delta;
	// if (cards[c].move.time < 0) {
	// cards[c].x = cards[c].move.new_x;
	// cards[c].y = cards[c].move.new_y;
	// cards[c].move.moving = false;
	// }
	// }
	// }
	// }
	// }
}
function render() {
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	for ( var card in cards) {
		if (cards.hasOwnProperty(card)) {
			var c = cards[card];
			if (c['face'] == 'front') {
				ctx.drawImage(c['front_img'], c['x'], c['y'], 108, 150);
			}
			if (c['face'] == 'back') {
				ctx.drawImage(c['back_img'], c['x'], c['y'], 108, 150);
			}
		}
	}
}
