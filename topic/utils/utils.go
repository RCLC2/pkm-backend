package utils

import (
	"encoding/json"
	"io"
	"sort"
)

func DecodeJSONResponse(r io.Reader, v interface{}) error {
	return json.NewDecoder(r).Decode(v)
}

func GetTopNFromMap(m map[string]int, n int) []string {
	type kv struct {
		Key string
		Val int
	}

	pairs := make([]kv, 0, len(m))
	for k, v := range m {
		pairs = append(pairs, kv{Key: k, Val: v})
	}

	sort.Slice(pairs, func(i, j int) bool {
		if pairs[i].Val == pairs[j].Val {
			return pairs[i].Key < pairs[j].Key
		}
		return pairs[i].Val > pairs[j].Val
	})

	limit := n
	if len(pairs) < limit {
		limit = len(pairs)
	}

	result := make([]string, 0, limit)
	for i := 0; i < limit; i++ {
		result = append(result, pairs[i].Key)
	}
	return result
}
