const http = require('http');
const fs = require('fs');
const path = require('path');

// Prevent uncaught errors from crashing the process
process.on('uncaughtException', (err) => {
  console.error('[HisabPro] Uncaught exception:', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('[HisabPro] Unhandled rejection at:', promise, 'reason:', reason);
});

// Listen on Cloud Run provided PORT, falling back to DEFAULT_APP_PORT (3000 in dev proxy) or 8080
const PORT = process.env.DEFAULT_APP_PORT
  ? parseInt(process.env.DEFAULT_APP_PORT, 10)
  : parseInt(process.env.PORT || process.env.APP_PORT || '8080', 10);

const PUBLIC_DIR = path.join(__dirname, 'public');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.txt': 'text/plain; charset=utf-8'
};

const server = http.createServer((req, res) => {
  const urlPath = req.url.split('?')[0];

  // Health check endpoint for Cloud Run and internal probes
  if (urlPath === '/health' || urlPath === '/healthz' || urlPath === '/_health') {
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    });
    res.end(JSON.stringify({ status: 'ok', app: 'HisabPro', timestamp: Date.now() }));
    return;
  }

  // Handle __cookie_check.html by serving index.html directly
  if (urlPath === '/__cookie_check.html' || urlPath.startsWith('/__cookie_check')) {
    const indexPath = path.join(PUBLIC_DIR, 'index.html');
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'public, max-age=3600'
    });
    fs.createReadStream(indexPath).pipe(res);
    return;
  }

  let filePath = path.join(PUBLIC_DIR, urlPath === '/' ? 'index.html' : urlPath);

  // Security check to prevent path traversal
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('403 Forbidden');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      // If the request has a specific file extension (like missing .png or .css), return 404
      const requestedExt = path.extname(urlPath);
      if (requestedExt && requestedExt !== '.html') {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end(`404 Not Found: ${urlPath}`);
        return;
      }
      // For all SPA navigation routes or root, fall back to index.html
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = (filePath.endsWith('manifest.json') || filePath.endsWith('manifest.webmanifest'))
      ? 'application/manifest+json; charset=utf-8'
      : (MIME_TYPES[ext] || 'application/octet-stream');

    const isSw = filePath.endsWith('sw.js');
    const headers = {
      'Content-Type': contentType,
      'Cache-Control': isSw ? 'no-cache, no-store, must-revalidate' : 'public, max-age=3600',
      'Service-Worker-Allowed': '/',
      'Access-Control-Allow-Origin': '*'
    };

    res.writeHead(200, headers);

    if (req.method === 'HEAD') {
      res.end();
      return;
    }

    const stream = fs.createReadStream(filePath);
    stream.on('error', (streamErr) => {
      console.error('[HisabPro] File stream error:', streamErr);
      if (!res.headersSent) {
        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('500 Internal Server Error');
      }
    });
    stream.pipe(res);
  });
});

server.on('error', (err) => {
  console.error('[HisabPro] Server error:', err);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[HisabPro] Production server listening on 0.0.0.0:${PORT}`);
});
