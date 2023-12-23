.PHONY: clean jar repl cli

clean:
	clj -T:build clean

jar:
	clj -T:build jar

repl:
	clj -M:repl

cli:
	clj -M:cli hello --name "Dudes"
