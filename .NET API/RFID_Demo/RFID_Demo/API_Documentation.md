# RFID System API Documentation

## Overview
This API provides authentication and CRUD operations for managing RFID inventory in a retail/clothing environment. The system includes features for fast RFID scanning, unique item identification, automatic data upload, and real-time record updates.

## Authentication
All API endpoints except `/api/auth/login` and `/api/auth/register` require JWT authentication.

### Endpoints

#### Authentication
- `POST /api/auth/login` - Authenticate user and get JWT token
- `POST /api/auth/register` - Register a new user

#### Products
- `GET /api/rfid/products` - Get all products
- `GET /api/rfid/products/{tagId}` - Get product by tag ID
- `POST /api/rfid/products` - Create new product
- `PUT /api/rfid/products/{id}` - Update product
- `DELETE /api/rfid/products/{id}` - Delete product

#### Scan Logs
- `GET /api/rfid/scanlogs` - Get all scan logs
- `GET /api/rfid/scanlogs/{tagId}` - Get scan logs for specific tag
- `POST /api/rfid/scanlogs` - Create new scan log
- `PUT /api/rfid/scanlogs/{id}` - Update scan log
- `DELETE /api/rfid/scanlogs/{id}` - Delete scan log

#### RFID Operations
- `POST /api/rfid/scan` - Simulate RFID scan and create log entry

## Request/Response Examples

### Login
```json
POST /api/auth/login
{
  "username": "admin",
  "password": "password123"
}
```

### Register
```json
POST /api/auth/register
{
  "username": "john_doe",
  "role": "employee",
  "password": "secure_password"
}
```

### Create Product
```json
POST /api/rfid/products
{
  "tagId": "300833B2DDD9014000000002",
  "productName": "Blue Jeans",
  "category": "Jeans",
  "size": "32",
  "color": "Blue"
}
```

### Record Scan
```json
POST /api/rfid/scan
{
  "tagId": "300833B2DDD9014000000001",
  "location": "Warehouse Entrance",
  "status": "IN"
}
```

## Features Implemented
- Fast RFID Scanning
- Unique Item Identification
- Automatic Data Upload
- Real-Time Record Update
- Time & Location Tracking
- Error Reduction
- Clothing Inventory Management
- Stock In/Out Monitoring
- Retail Billing Support
- Loss & Theft Control
- Warehouse Auditing