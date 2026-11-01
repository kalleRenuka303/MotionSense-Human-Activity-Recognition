# Contributing to MotionSense

Thank you for your interest in contributing to **MotionSense — Human Activity Recognition Using Smartphones**! 

We welcome contributions from researchers, machine learning practitioners, and Android mobile developers.

---

## Code of Conduct

Please maintain a collaborative, respectful, and professional environment. Focus on technical rigor, reproducible results, and high code quality.

---

## Development Workflow

1. **Fork the Repository**:
   Create your personal fork and clone it locally.

2. **Branch Naming**:
   - `feat/feature-name` for new capabilities.
   - `fix/bug-description` for bug fixes.
   - `docs/documentation-update` for documentation changes.
   - `test/test-description` for new unit/integration tests.

3. **Android Development Guidelines**:
   - Write clean, idiomatic Kotlin code following official Android architectural patterns.
   - Use Jetpack Compose Material 3 for all UI components.
   - Do not perform heavy computations, filtering, or ONNX inference on the Main thread.
   - Always verify that all unit tests pass:
     ```bash
     cd android && ./gradlew test
     ```
   - Ensure APKs build cleanly without compiler or lint errors:
     ```bash
     ./gradlew assembleDebug assembleRelease
     ```

4. **Machine Learning & Python Guidelines**:
   - Models must be exported in ONNX format targeting `opset_version=14` or higher.
   - Verify numerical parity between PyTorch outputs and ONNX Runtime outputs ($< 10^{-5}$ max difference).
   - Never perform random train/test splits that leak subject data across partitions.

5. **Commit Messages**:
   Follow the Conventional Commits specification:
   - `feat(mobile): ...`
   - `fix(pipeline): ...`
   - `docs(readme): ...`
   - `test(sensor): ...`

---

## Submitting Pull Requests

1. Push your branch to your fork.
2. Submit a Pull Request targeting the `develop` branch.
3. Provide a clear description of changes, motivation, and verification steps (e.g., test command outputs, APK build status).
4. Ensure all CI checks pass.
