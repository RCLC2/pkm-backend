package main

import (
	"context"
	"log"
	"os"

	"github.com/yorkie-team/yorkie/admin"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"graph/server"
	"graph/services"
)

func main() {
	mongoURI := os.Getenv("MONGO_URI")
	if mongoURI == "" {
		log.Fatal("MONGO_URI environment variable not set")
	}
	dbName := os.Getenv("MONGO_DB_NAME")
	if dbName == "" {
		log.Fatal("MONGO_DB_NAME environment variable not set")
	}

	client, err := mongo.Connect(context.TODO(), options.Client().ApplyURI(mongoURI))
	if err != nil {
		log.Fatal(err)
	}
	defer client.Disconnect(context.TODO())

	db := client.Database(dbName)

	yorkieAddr := os.Getenv("YORKIE_ADDR")
	if yorkieAddr == "" {
		log.Fatal("YORKIE_ADDR environment variable not set")
	}

	yorkieAdmin, err := admin.Dial(
		yorkieAddr,
		admin.WithInsecure(true),
	)
	if err != nil {
		log.Fatalf("Failed to connect to Yorkie server: %v", err)
	}
	defer yorkieAdmin.Close()

	username := os.Getenv("YORKIE_ADMIN_USERNAME")
	password := os.Getenv("YORKIE_ADMIN_PASSWORD")

	if username != "" && password != "" {
		token, err := yorkieAdmin.LogIn(context.Background(), username, password)
		if err != nil {
			log.Fatalf("Failed to login: %v", err)
		}

		yorkieAdmin.Close()
		yorkieAdmin, err = admin.Dial(
			yorkieAddr,
			admin.WithInsecure(true),
			admin.WithToken(token),
		)
		if err != nil {
			log.Fatalf("Failed to reconnect with token: %v", err)
		}
	}

	version, err := yorkieAdmin.GetServerVersion(context.Background())
	if err != nil {
		log.Fatalf("Failed to connect to Yorkie server: %v", err)
	}
	log.Printf("Connected to Yorkie server version: %s", version.YorkieVersion)

	graphService := services.NewGraphService(db)
	workspaceService := services.NewWorkspaceService(db, graphService, yorkieAdmin)
	router := server.NewServer(graphService, workspaceService)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	log.Printf("Server is running on port %s", port)

	if err := router.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}
