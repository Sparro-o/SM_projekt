# SM_projekt
**TO DO:**
- usunąć komentarze
- usunąć niepotrzebne pliki .kt z API i com.example
- usunąć zbędne porady o zmianie pytań
- zrobić aby X i Y rote zmieniał pytanie
 
**WYKORZYSTANE PLIKI**

✅ MainActivity.kt - główna aktywność, logika aplikacji, obsługa UI i gestów
✅ GestureDetector.kt - wykrywanie potrząsania i przechyleń telefonu

Lokalizacja: app/src/main/java/com/example/quiz_projekt/api/

✅ TriviaQuestion.kt - modele danych (data classes) dla pytań z API
✅ TriviaApiService.kt - interface definiujący endpoint API
✅ RetrofitClient.kt - konfiguracja klienta HTTP (Retrofit)


2. Pliki XML (interfejs użytkownika)
Lokalizacja: app/src/main/res/layout/

✅ activity_main.xml - układ ekranu (przyciski, teksty, animacja Lottie)

Lokalizacja: app/manifests/

✅ AndroidManifest.xml - uprawnienia (Internet), konfiguracja Activity


3. Plik animacji Lottie
Lokalizacja: app/src/main/res/raw/

✅ quiz_animation.json - animacja JSON z LottieFiles


4. Pliki konfiguracyjne Gradle
Lokalizacja: app/

✅ build.gradle.kts (Module: app) - zależności projektu (Retrofit, Lottie, Gson)

Lokalizacja: główny folder projektu

✅ settings.gradle.kt - repozytoria (Maven, Google, JitPack - jeśli dodawałeś)

**LINK DO CLAUDE**
https://claude.ai/share/5a0656cd-41a4-44dd-8bef-36c979a95494
