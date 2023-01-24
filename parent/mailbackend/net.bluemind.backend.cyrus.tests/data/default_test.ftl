require ["imapflags"];

# vacation
if allof (not address :matches "from" "noreply*") {
	setflag "\\Flagged";
}

# END
