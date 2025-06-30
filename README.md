# PennyPincher

PennyPincher is a modern personal finance management app for Android, built with Jetpack Compose and Material 3. It helps users track expenses, manage income, organize categories, and gain insights into their financial habits.

## Features

- **Expense Tracking:** Add, edit, and categorize expenses with payment methods and notes.
- **Income Management:** Log income sources, amounts, and dates.
- **Category System:** Organize expenses and income into parent and child categories for detailed tracking.
- **Payment Methods:** Manage and assign payment methods to transactions.
- **Visual Insights:** View your spending and income with bar and pie charts.
- **Modern UI:** Clean, responsive interface using Material 3 and Jetpack Compose.
- **Dark/Light Theme:** Seamless support for both dark and light modes.
- **Firebase Integration:** (If enabled) for authentication and data sync.

## Getting Started

### Prerequisites
- Android Studio (Giraffe or newer recommended)
- Android SDK 33+
- Kotlin 1.8+

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/neski321/PennyPincher.git
   cd PennyPincher
   ```
2. **Open in Android Studio:**
   - Open the project folder in Android Studio.
3. **Configure Firebase (Optional):**
   - Add your `google-services.json` to `app/` if using Firebase features.
4. **Build and Run:**
   - Click 'Run' or use `Shift+F10` to build and launch the app on an emulator or device.

## Project Structure

- `app/src/main/java/com/neski/pennypincher/`
  - `ui/` — Compose UI screens and components
  - `data/` — Models and repositories
  - `theme/` — Color, typography, and theme setup
  - `res/` — Resources (icons, strings, etc.)

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository
2. Create a new branch (`git checkout -b feature/your-feature`)
3. Commit your changes
4. Push to your fork and submit a pull request

Please open an issue for bugs or feature requests.

_Made with ❤️ using Jetpack Compose._ 