option solver kestrel;
option kestrel_options 'solver=donlp2';

set rcc;
set I;

param time {rcc};
param nCalls {rcc};
param parent {I} symbolic within rcc;
param rccOf {I} symbolic within rcc;

var f {I} >= 0;

# Error is f[i] - 'optimal' f[i]
minimize fError {i in I}:
	( f[i] - nCalls[parent[i]] / sum {j in I: rccOf[j] == rccOf[i]} nCalls[parent[j]] ) ** 2;

#minimize sumFError {r in rcc : card( { i in I : rccOf[i] == r } ) > 0 }:
#	1 - sum {i in I : rccOf[i] == r} f[i];

subject to sumF {r in rcc : card( { i in I : rccOf[i] == r } ) > 0 }:
	sum {i in I : rccOf[i] == r} f[i] == 1;

subject to childTime {r in rcc : card( { i in I : parent[i] == r } ) > 0 }:
	sum {i in I : parent[i] == r} f[i] * time[rccOf[i]] <= time[r] * 1.03;

