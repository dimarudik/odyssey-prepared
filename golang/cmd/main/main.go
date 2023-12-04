package main

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5"
	"os"
)

func main() {
	connectionString := os.Args[1]
	conn, err := pgx.Connect(context.Background(), connectionString)
	onError(err)
	defer closeConnection(conn, context.Background())
	pgConn := conn.PgConn()

	var isRecovery bool
	err = conn.QueryRow(context.Background(), "select pg_is_in_recovery()").Scan(&isRecovery)
	onError(err)

	fmt.Printf("%d %t\n", pgConn.PID(), isRecovery)
}

func closeConnection(conn *pgx.Conn, ctx context.Context) {
	err := conn.Close(ctx)
	onError(err)
}

func onError(err error) {
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
