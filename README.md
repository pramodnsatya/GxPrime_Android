# GxPrime Android - Validator Assessment Platform

A comprehensive Android application for conducting GxPrime assessments with role-based access control and enterprise management.

## 🚀 Features

### Core Functionality
- **Multi-Role Support**: Super Admin, Enterprise Admin, and Regular Users
- **Assessment Management**: Create, conduct, and manage assessments across multiple domains
- **Enterprise Management**: Complete enterprise lifecycle management
- **Facility Management**: Track and manage assessment facilities
- **User Permissions**: Granular role-based access control
- **Assessment History**: Comprehensive reporting and analytics

### Assessment Domains
- **Quality Unit**: Quality assurance and compliance assessments
- **Packaging & Labeling**: Product packaging and labeling compliance
- **Production**: Manufacturing and production process assessments
- **Materials**: Material management and quality control
- **Laboratory**: Laboratory testing and validation
- **Facilities & Equipment**: Infrastructure and equipment assessments

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/pramod/validator/
├── data/
│   ├── models/           # Data models (User, Enterprise, Facility, etc.)
│   ├── repository/       # Data access layer
│   └── QualityUnitQuestions.kt
├── navigation/           # Navigation configuration
├── ui/
│   ├── components/       # Reusable UI components
│   ├── screens/          # Application screens
│   └── theme/           # Material 3 theming
├── utils/               # Utility classes
├── viewmodel/           # ViewModels for state management
└── MainActivity.kt
```

### Key Components

#### Data Layer
- **FirebaseRepository**: Main data access for Firebase operations
- **FacilityRepository**: Facility-specific operations
- **PermissionRepository**: User permission management

#### UI Layer
- **Material 3 Design**: Modern, consistent UI components
- **Compose Navigation**: Type-safe navigation
- **Role-based UI**: Dynamic interface based on user permissions

#### Business Logic
- **ViewModels**: State management and business logic
- **Repository Pattern**: Clean separation of data access
- **Role-based Access**: Granular permission system

## 🔧 Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Backend**: Firebase (Firestore, Authentication, Storage)
- **Navigation**: Compose Navigation
- **State Management**: StateFlow, Compose State
- **Material Design**: Material 3

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8.0+
- Firebase project setup

### Setup
1. Clone the repository
2. Configure Firebase:
   - Add `google-services.json` to `app/` directory
   - Configure Firestore security rules
3. Build and run the application

### Firebase Configuration
- **Authentication**: Email/password authentication
- **Firestore**: Document-based database
- **Storage**: PDF and video resources
- **Security Rules**: Role-based access control

## 📱 User Roles

### Super Admin
- System-wide access
- Enterprise management
- User management
- Question database management

### Enterprise Admin
- Enterprise-specific management
- User creation and permissions
- Facility management
- Assessment oversight

### Regular Users
- Assessment creation and completion
- Personal assessment history
- Role-based domain access

## 🔒 Security Features

- **Role-based Access Control**: Granular permissions
- **Data Isolation**: Enterprise-scoped data access
- **Secure Authentication**: Firebase Auth integration
- **Permission Validation**: Server-side permission checks

## 📊 Assessment Features

- **Multi-Domain Support**: 6 assessment domains
- **Comprehensive Questions**: 200+ assessment questions
- **Real-time Progress**: Live assessment tracking
- **Detailed Reporting**: Comprehensive assessment reports
- **AI Integration**: OpenAI-powered assessment summaries

## 🛠️ Development

### Code Quality
- **Clean Architecture**: Separation of concerns
- **Material 3**: Modern design system
- **Type Safety**: Kotlin null safety
- **Performance**: Optimized Compose UI

### Testing
- Unit tests for business logic
- UI tests for critical user flows
- Integration tests for Firebase operations

## 📈 Performance

- **Optimized Queries**: Efficient Firestore queries
- **Lazy Loading**: On-demand data loading
- **Caching**: Local state management
- **Memory Management**: Proper lifecycle handling

## 🔄 Recent Updates

- **Hard Delete**: Permanent data removal for security
- **Warning Dialogs**: User confirmation for destructive actions
- **Modern UI**: Material 3 design implementation
- **Code Cleanup**: Removed debug files and improved structure
- **Performance**: Optimized repository operations

## 📝 License

This project is proprietary software. All rights reserved.

## 🤝 Contributing

For development guidelines and contribution policies, please contact the development team.

---

**Built with ❤️ for GxPrime Assessment Platform**
