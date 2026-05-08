# Remote Care Static Site

Static HTML, CSS, and JS landing page.

## Preview

Open `index.html` in a browser or serve it with a simple HTTP server.

## Deploy

Copy the contents of this folder to the Nginx web root:

```bash
sudo rsync -av --delete --exclude '.well-known' /var/www/Remote-Healthcare/web-static/ /var/www/remote-care/
```
