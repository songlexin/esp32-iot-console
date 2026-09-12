FROM node:22-alpine
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --omit=dev
COPY index.js ./
ENV MQTT_HOST=mosquitto
ENV MQTT_PORT=1883
ENV DEVICE_COUNT=2
CMD ["npm", "start"]
