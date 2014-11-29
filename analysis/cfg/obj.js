
var stack = function() {
	
	var push =  function(x) { 
		this.top = {next: this.top, data : x}; 
		return x;
	};
	
	var pop = function() { 
    	var y = this.top.data; 
    	this.top = this.top.next; 
    	return y; 
    }
	
	return {
		top : null,
	    push: push,
	    pop: pop
	}
}

var s = stack();
s.push(1);
s.push(2);
var d = s.pop();