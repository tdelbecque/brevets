use strict;
use diagnostics;

my @Lines;
my $Page = 0;
my $Blank = 0;

while (<>) {
    if (/\w/) {
	$Blank = 0;
	if (/\xa9 QUESTEL/) {
	    $Page = 1;
	} else {
	    s/\s*[\x0d\x0a]+//;
	    if ($Page) {
		push @Lines, "Page $_";
		$Page = 0;
	    } else {
		push @Lines, $_;
	    }
	}
    } elsif (! $Blank) {
	$Blank = 1;
	push @Lines, "";
    }
}

my ($inAbstract, $code, $assignee, $title, @inventors, @abstract) = (0);

for (my $i = 0; $i < @Lines; $i ++) {
    my $l = $Lines [$i];
    if ($l =~ /^\xe2\x80\xa2 Patent Assignee/) {
	my $j = $i - 1;
	for (; $Lines [$j] !~ /^Page/; $j --)  {}
	$j += 2;
	$title = '';
	@inventors = ();
	
	for (; $Lines [$j] !~ /\d$/; $j ++) {
	    $title = "$title $Lines[$j]";
	}
	next;
    }
    if ($l =~ /^\xe2\x80\xa2 Abstract/) {
	$code = $Lines [++$i];
	$inAbstract = 1;
	next;
    }
    if ($inAbstract) {
	if ($l eq "") {
	    my $abstract = join " ", @abstract;
	    $abstract =~ s/^Questel Machine translated Abstract//;
	    $abstract =~ s/\(.+?\)//g;
	    $abstract =~ s/\s+/ /g;
	    print "$code\t$title\t$abstract\n";
	    $inAbstract = 0;
	    @abstract = ();
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

