package main

import (
	"log"
	"os"
	"topic/esclient"
	"topic/server"

	"github.com/elastic/go-elasticsearch/v8"
)

func main() {
	addr := os.Getenv("ELASTICSEARCH_ADDR")
	if addr == "" {
		addr = "http://elasticsearch:9200"
	}

	index := os.Getenv("ELASTICSEARCH_INDEX")
	if index == "" {
		index = "pkm-note"
	}

	port := os.Getenv("SERVER_PORT")
	if port == "" {
		port = "8082"
	}

	cfg := elasticsearch.Config{
		Addresses: []string{addr},
	}

	es, err := esclient.NewElasticService(cfg, index)
	if err != nil {
		log.Fatalf("Error initializing Elasticsearch: %s", err)
	}

	srv := server.NewServer(es)
	if err := srv.Run(":" + port); err != nil {
		log.Fatalf("Error starting server: %s", err)
	}
}
