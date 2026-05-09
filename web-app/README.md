# Remote Care Web App

React + Vite marketing site for Remote Care.

## Local development

```bash
cd /var/www/Remote-Healthcare/web-app
npm install
npm run dev
```

## Build

```bash
cd /var/www/Remote-Healthcare/web-app
npm run build
```

## Deploy

Copy the build output to the Nginx web root:

```bash
cd /var/www/Remote-Healthcare/web-app
npm run build
sudo rsync -av --delete --exclude '.well-known' dist/ /var/www/remote-care/
```
