import urllib.request
import re

url = "https://pokemonbattlearena.net/members/mapguide.php"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        
        matches = re.finditer(r'(.{0,150}Route.{0,150})', html, re.IGNORECASE)
        for i, m in enumerate(matches):
            print(f"Route context {i}: {m.group(1)}")
            if i > 5: break
except Exception as e:
    print(f"Error: {e}")
