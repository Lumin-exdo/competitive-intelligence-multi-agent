package tools

import (
	"crypto/sha256"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/PuerkitoBio/goquery"
)

func FetchAndExtractText(url string) (string, string, error) {
	client := &http.Client{Timeout: 30 * time.Second}

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", "", err
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (compatible; CIBot/1.0)")

	resp, err := client.Do(req)
	if err != nil {
		return "", "", err
	}
	defer resp.Body.Close()

	doc, err := goquery.NewDocumentFromReader(resp.Body)
	if err != nil {
		return "", "", err
	}

	doc.Find("script, style, noscript").Remove()
	text := strings.TrimSpace(doc.Find("body").Text())

	hash := fmt.Sprintf("%x", sha256.Sum256([]byte(text)))
	return text, hash, nil
}
