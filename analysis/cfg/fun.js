
var f = function(x) { return x(1); }
var g = function(y) { return y+2; }
var h = function(z) { return z+3; }

f(g) + f(h);