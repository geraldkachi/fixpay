FROM nginx:alpine

# Copy built assets from builder stage (or CI/CD)
# We assume the CI/CD pipeline runs `npm run build` and produces the `dist` folder.
COPY ./dist /usr/share/nginx/html

# Add a basic Nginx configuration for single-page apps (SPA routing)
RUN rm /etc/nginx/conf.d/default.conf
COPY ./nginx.conf /etc/nginx/conf.d/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
