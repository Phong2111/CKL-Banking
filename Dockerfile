FROM node:24-alpine

# Install Java 21 (required for Firestore emulator)
# Firebase tools requires Java 21+
RUN apk add --no-cache openjdk21-jre

# Install Firebase CLI globally
RUN npm install -g firebase-tools

# Set working directory
WORKDIR /app

# Copy package files
COPY functions/package*.json ./functions/

# Install dependencies
WORKDIR /app/functions
RUN npm install

# Set working directory back to root
WORKDIR /app

# Copy firebase.json
COPY firebase.json ./

# Expose ports
EXPOSE 4000 5001 8080 9099

# Start emulators
CMD ["sh", "-c", "firebase emulators:start --only functions,firestore --import=./emulator-data --export-on-exit"]

