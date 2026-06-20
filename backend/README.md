## 🔐 Authentication Setup (Keycloak & Postman)

This project uses Keycloak as the Identity and Access Management (IAM) server. The API Gateway acts as an OAuth2 Resource Server. Follow these steps to create a local user and configure Postman to access the secured API endpoints.

### Step 1: Start Keycloak
Ensure your Docker containers are running. Keycloak will automatically import the `healthmonitor-realm` configuration on startup using the provided `realm-export.json` file.

### Step 2: Create a Test User in Keycloak
For security reasons, user accounts are not exported with the realm configuration. Each developer needs to create their own local user for testing.

1. Open the Keycloak Admin Console in your browser: [http://localhost:9090](http://localhost:9090)
2. Log in using the default local credentials:
   * **Username:** `admin`
   * **Password:** `admin`
3. In the top-left dropdown menu, ensure **`healthmonitor-realm`** is selected.
4. In the left sidebar menu, navigate to **Users** and click the **Add user** button.
5. Provide a username, email, name and last name and click **Save**.
6. Go to the **Credentials** tab for this newly created user.
7. Click the **Set Password** (or Reset Password) button.
8. Enter a simple password (e.g., `password123`).
9. ⚠️ **CRITICAL STEP:** Toggle the **Temporary** switch to **OFF**. *(If you leave it on, API requests via Postman will be blocked with an "Account is not fully set up" error).*
10. Click **Save** and confirm the action in the red warning popup.

---

### Step 3: Configure Postman & Test the API
To pass through the API Gateway, you need to configure Postman to automatically fetch a JWT token from Keycloak.

1. Open Postman and select your API request (e.g., `GET http://localhost:8080/api/v1/patients/hello`).
2. Go to the **Authorization** tab located just below the request URL.
3. In the **Type** dropdown menu, select **OAuth 2.0**.
4. Scroll down to the **Configure New Token** section and fill out the form exactly as follows:
   * **Token Name:** `HealthMonitor Token` *(or any name you prefer)*
   * **Grant Type:** `Password Credentials`
   * **Access Token URL:** `http://localhost:9090/realms/healthmonitor-realm/protocol/openid-connect/token`
   * **Client ID:** `health-api`
   * **Client Secret:** *(Leave this field completely blank)*
   * **Username:** The username you created in Step 2 (e.g., `testuser`)
   * **Password:** The password you set in Step 2 (e.g., `password123`)
5. Click the orange **Get New Access Token** button at the bottom.
6. A success window will pop up displaying your generated JWT token. Click the **Use Token** button.
7. Click **Send** to execute your API request. The Gateway will now authenticate your token and grant you access to the microservices!




### How to Simulate iot measurement
To simulate iot measuremnt flow, create containers by Docker Compose:
```
docker-compose up -d
```

To get realtime measurement simulation in case testing ML working use command with custom params:
```
docker-compose --profile simulation run --rm simulator --mode batch --patient-id "patient_123" --days 2 
```

To get batch measurement simulation in case trainning LSTM model use command with custom params:
```
docker-compose --profile simulation run --rm simulator --mode realtime --patient-id "patient_12345" --interval 2
```

When you generated batch data or waited in real time mode as long as you get over 1000 records in db (not recommended), use this command if you want to train LSTM model:
```
docker-compose up -d ai-trainer 
```

If you recently trained new LSTM model, you have to restart ai-worker container by following command:
```
docker-compose up -d ai-worker 
```

Enjoy


