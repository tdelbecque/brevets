use strict;
use diagnostics;

my @Lines;
my $Page = 0;

while (<>) {
    if (/\w/) {
	if (/\xa9 QUESTEL/) {
	    $Page = 1;
	} else {
	    s/\s*\x0d\x0a//;
	    if ($Page) {
		push @Lines, "Page $_";
		$Page = 0;
	    } else {
		push @Lines, $_;
	    }
	}
    }
}

my ($inAbstract, $code, $assignee, $title, @inventors, @abstract) = (0);

for (my $i = 0; $i < @Lines; $i ++) {
    my $l = $Lines [$i];
    if ($l =~ /^ Patent Assignee/) {
	$code = $Lines [$i - 1];
	$title = '';
	@inventors = ();
       	for (my $j = $i - 2; $Lines [$j] !~ /^Page \d/; $j --) {
	    $title = "$Lines[$j] $title";
	}
	next;
    }
    if ($l =~ /^ Abstract/) {
	$i ++;
	$inAbstract = 1;
	@abstract = ();
	next;
    }
    if ($inAbstract) {
	if ($l =~ /^Page/ || $l =~ /\W*\[/) {
	    $inAbstract = 0;
	    my $abstract = join " ", @abstract;
	    $abstract =~ s/^Questel Machine translated Abstract//;
	    $abstract =~ s/\(.+?\)//g;
	    $abstract =~ s/\s+/ /g;
	    print "$code\t$title\t$abstract\n";
	} else {
	    push @abstract, $l
	}
    }
}

if  (@abstract) {
    my $abstract = join " ", @abstract;
    $abstract =~ s/^Questel Machine translated Abstract//;
    $abstract =~ s/\(.+?\)//g;
    $abstract =~ s/\s+/ /g;
    print "$code\t$title\t$abstract\n";
}

