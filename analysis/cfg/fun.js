
var a = 2;
var b = 3;

var f = function(x) { return x(1); }
var g = function(y) { return y + a; }
var h = function(z) { return z + b; }

f(g) + f(h);