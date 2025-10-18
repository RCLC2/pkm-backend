package esclient

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"

	"topic/utils"

	"github.com/elastic/go-elasticsearch/v8"
	"github.com/elastic/go-elasticsearch/v8/esapi"
)

type ElasticService struct {
	client    *elasticsearch.Client
	indexName string
}

func NewElasticService(cfg elasticsearch.Config, indexName string) (*ElasticService, error) {
	cli, err := elasticsearch.NewClient(cfg)
	if err != nil {
		return nil, err
	}
	return &ElasticService{client: cli, indexName: indexName}, nil
}

func (es *ElasticService) handleESResponse(res *esapi.Response, prefix string) (io.Reader, error) {
	if res.IsError() {
		buf := new(bytes.Buffer)
		io.Copy(buf, res.Body)
		return nil, fmt.Errorf("%s: %s", prefix, buf.String())
	}
	return res.Body, nil
}

func (es *ElasticService) FindSimilarDocsByContent(ctx context.Context, content string) ([]string, error) {
	if strings.TrimSpace(content) == "" {
		return nil, errors.New("content is empty")
	}

	query := map[string]interface{}{
		"size": 100,
		"query": map[string]interface{}{
			"more_like_this": map[string]interface{}{
				"fields":          []string{"content"},
				"like":            []interface{}{content},
				"min_term_freq":   1,
				"max_query_terms": 25,
			},
		},
	}

	return es.executeSimilaritySearch(ctx, query)
}

func (es *ElasticService) FindSimilarDocsById(ctx context.Context, docId string) ([]string, error) {
	if strings.TrimSpace(docId) == "" {
		return nil, errors.New("docId is empty")
	}

	query := map[string]interface{}{
		"size": 100,
		"query": map[string]interface{}{
			"more_like_this": map[string]interface{}{
				"fields": []string{"content"},
				"like": []interface{}{
					map[string]interface{}{
						"_index": es.indexName,
						"_id":    docId,
					},
				},
				"min_term_freq":   1,
				"max_query_terms": 25,
			},
		},
	}

	return es.executeSimilaritySearch(ctx, query)
}

func (es *ElasticService) executeSimilaritySearch(ctx context.Context, query map[string]interface{}) ([]string, error) {
	b, err := json.Marshal(query)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal query: %w", err)
	}

	res, err := es.client.Search(
		es.client.Search.WithContext(ctx),
		es.client.Search.WithIndex(es.indexName),
		es.client.Search.WithBody(bytes.NewReader(b)),
	)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	body, err := es.handleESResponse(res, "es search error")
	if err != nil {
		return nil, err
	}

	var parsed struct {
		Hits struct {
			Hits []struct {
				ID    string  `json:"_id"`
				Score float64 `json:"_score"`
			} `json:"hits"`
		} `json:"hits"`
	}

	if err := utils.DecodeJSONResponse(body, &parsed); err != nil {
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	minScore := 70.0 // 70% 이상 유사해야 포함
	ids := make([]string, 0, len(parsed.Hits.Hits))
	for _, h := range parsed.Hits.Hits {
		if h.Score >= minScore {
			ids = append(ids, h.ID)
		}
	}
	return ids, nil
}

func (es *ElasticService) ExtractTopTags(ctx context.Context, content string, topN int) ([]string, error) {
	if strings.TrimSpace(content) == "" {
		return nil, errors.New("content is empty")
	}
	if topN <= 0 {
		topN = 10
	}

	payload := map[string]interface{}{
		"analyzer": "nori_analyzer",
		"text":     content,
	}
	b, err := json.Marshal(payload)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal: %w", err)
	}

	res, err := es.client.Indices.Analyze(
		es.client.Indices.Analyze.WithContext(ctx),
		es.client.Indices.Analyze.WithIndex(es.indexName),
		es.client.Indices.Analyze.WithBody(bytes.NewReader(b)),
	)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	body, err := es.handleESResponse(res, "analyze error")
	if err != nil {
		return nil, err
	}

	var anal struct {
		Tokens []struct {
			Token string `json:"token"`
		} `json:"tokens"`
	}

	if err := utils.DecodeJSONResponse(body, &anal); err != nil {
		return nil, fmt.Errorf("failed to parse: %w", err)
	}

	freq := map[string]int{}
	for _, t := range anal.Tokens {
		tok := strings.TrimSpace(t.Token)
		if len([]rune(tok)) <= 1 {
			continue
		}
		freq[tok]++
	}

	tags := utils.GetTopNFromMap(freq, topN)
	return tags, nil
}
