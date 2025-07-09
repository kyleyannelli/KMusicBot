#!/usr/bin/env bash
set -euo pipefail

function get_vid() {
    VID="${1:-}"
    [ -z "$VID" ] && { echo "Usage: $0 <YouTube Video ID>" >&2; exit 1; }

    HTML=$(curl -s "https://www.youtube.com/watch?v=$VID")

    KEY=$(echo "$HTML" | sed -n 's/.*"INNERTUBE_API_KEY":"\([^"]*\)".*/\1/p' | head -n1)
    VERSION=$(echo "$HTML" | sed -n 's/.*"INNERTUBE_CONTEXT_CLIENT_VERSION":"\([^"]*\)".*/\1/p' | head -n1)

    [ -z "$KEY" ] && { echo "Failed to extract API key" >&2; exit 1; }

    curl -s "https://www.youtube.com/youtubei/v1/next?key=$KEY" \
        -H 'content-type: application/json' \
        -H "x-youtube-client-name: 1" \
        -H "x-youtube-client-version: $VERSION" \
        --data-raw '{
            "context": {
                "client": {
                    "hl": "en",
                    "gl": "US",
                    "clientName": "WEB",
                    "clientVersion": "'"$VERSION"'"
                }
        },
    "videoId": "'"$VID"'",
    "params": "wAEB"
}' | jq -r '
.contents.twoColumnWatchNextResults.secondaryResults.secondaryResults.results[]
| if .lockupViewModel then
[ .lockupViewModel.contentId,
(.lockupViewModel.metadata.lockupMetadataViewModel.title.content // "NO TITLE") ]
elif .compactVideoRenderer then
    [ .compactVideoRenderer.videoId,
    (.compactVideoRenderer.title.runs[0].text // "NO TITLE") ]
elif .reelItemRenderer then
    [ .reelItemRenderer.reelWatchEndpoint.videoId, "(Shorts – no title)" ]
else empty
end
| @tsv'

}

get_vid "$1" | awk '{ print $1 }' | grep -E '^[a-zA-Z0-9]{11}$' | awk '{print "https://youtube.com/watch?v=" $0}'
