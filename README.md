# Uzbek TTS — Android ilova

Matcha-TTS asosidagi o'zbekcha ONNX ovoz modeli uchun to'liq funksional Android ilova.
Kotlin + Jetpack Compose, ONNX Runtime Mobile orqali qurilmada (on-device) ishlaydi.

Matn tozalash, belgi-lug'at va model kirish/chiqish formati
**[Ovozify-Labs/text-to-speech-ui](https://github.com/Ovozify-Labs/text-to-speech-ui)**
repozitoriyasidagi `onnx_infer.py`, `infer_utils.py`, `symbols.py` va `cleaners.py`
fayllariga aynan mos qilib portlangan (Kotlinga to'g'ridan-to'g'ri ko'chirilgan).

## Xususiyatlari
- Matn kiritib, ovozga aylantirish (offline, qurilmada)
- Natijani tinglash, saqlash va ulashish (share)
- Sintez tarixi (Room bazasi)
- Sozlamalar: nutq tezligi (speaking_rate), ovoz xilma-xilligi (temperature/noise_scale), mavzu
- Model birinchi ishga tushirishda avtomatik yuklanadi (uzilishda davom ettirish bilan) va keyin to'liq oflayn ishlaydi

## Model bilan mosligi (tasdiqlangan)
`onnx_infer.py`ga asosan:
- **Kirishlar**: `x` (int64, `[1, seq_len]`), `x_lengths` (int64, `[1]`), `scales` (float32, `[2]` = `[temperature, speaking_rate]`), va agar model ko'p-spikerli bo'lsa (4 ta kirish) — `spks` (int64, `[1]`, standart qiymat 0)
- **Chiqishlar**: `wavs, wav_lengths = model.run(None, inputs)` — ikkita chiqish, nomi bilan emas, **tartib bo'yicha** o'qiladi (chunki asl kodda ham shunday), so `TTSEngine.kt` `session.outputNames`dan birinchi ikkitasini tartib bo'yicha oladi
- **Matn tozalash**: `basic_cleaners` — `x`→`h` almashtirish (agar `s`dan keyin bo'lmasa), egri qo'shtirnoqlarni to'g'irlash, faqat harf/raqam/bo'shliq/`,.?!'` qoldirish, raqamlarni o'zbekcha so'zlarga aylantirish, kichik harflarga o'tkazish
- **Belgi-lug'at**: `symbols.py`dagi `_pad + _punctuation + _letters` ro'ychasi (IPA harflar ishlatilmagani uchun tashlab yuborilgan — ular baribir ishlatilmaydi va lug'atning oxirida joylashgan, shuning uchun ID'larga ta'sir qilmaydi)
- **Intersperse**: `infer_utils.py`dagidek, har bir belgi orasiga va boshi/oxiriga `pad` (id=0) qo'yiladi

## Model manbai (tasdiqlangan)

Model **[OvozifyLabs/matcha-tts-uz-v1](https://huggingface.co/OvozifyLabs/matcha-tts-uz-v1)**
(Hugging Face, `model.onnx`, ~130 MB) — `Config.kt`da shu havola allaqachon sozlangan,
qo'shimcha hech narsa o'zgartirish shart emas.

Agar modelingiz ko'p-spikerli (multi-speaker) bo'lsa va standart bo'lmagan speaker ID kerak bo'lsa,
`Config.DEFAULT_SPEAKER_ID`ni o'zgartiring.

## Lokal build qilish (Android Studio)
1. Android Studio (Koala yoki undan yangi) da loyihani oching
2. Gradle sync avtomatik ishga tushadi
3. `Run` tugmasini bosing (min SDK 24, target SDK 34)

## Loyiha tuzilishi
```
app/src/main/java/com/uzbekai/tts/
  Config.kt                  # Model URL, tensor nomlari, sample rate
  UzbekTTSApp.kt              # Application-darajadagi singletonlar
  MainActivity.kt
  data/
    ModelManager.kt           # ONNX faylni yuklab olish (resume bilan)
    Tokenizer.kt               # symbols.py + cleaners.py + intersperse porti
    TTSEngine.kt                # ONNX Runtime inference + WAV yozish
    SettingsRepository.kt       # DataStore sozlamalari
    db/                         # Room: tarix
  ui/
    theme/, navigation/, screens/, MainViewModel.kt
```

## GitHub orqali APK yasash (Actions)

Loyihada ikkita tayyor workflow bor — build qilish uchun Android Studio shart emas.

### 1) Har bir push’da avtomatik APK (`.github/workflows/build-apk.yml`)
Reponi GitHub’ga push qilishingiz bilan (yoki `main` branchga PR ochsangiz) ishga tushadi:
1. GitHub’da **Actions** bo'limiga o'ting
2. "Build APK" workflow ishlashini kuting (~3-5 daqiqa)
3. Tugagach, pastdagi **Artifacts** bo'limidan `uzbek-tts-debug-apk` faylni yuklab oling — ichida to'g'ridan-to'g'ri telefonga o'rnatsa bo'ladigan `.apk` bor (debug-kalit bilan avtomatik imzolangan)

### 2) Rasmiy versiya chiqarish (`.github/workflows/release-apk.yml`)
Tag bosib push qilsangiz, GitHub Release avtomatik yaratiladi va APK unga biriktiriladi:
```bash
git tag v1.0.0
git push origin v1.0.0
```
Keyin repo'ning **Releases** sahifasida APK tayyor bo'ladi.

**Imzolangan (Play Store’ga tayyor) release APK** kerak bo'lsa, quyidagi repo secretlarini qo'shing
(Settings → Secrets and variables → Actions):
- `RELEASE_KEYSTORE_BASE64` — keystore faylingizni `base64` qilib qo'ying (`base64 -w0 my.keystore`)
- `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

Bu secretlar bo'lmasa ham muammo emas — release workflow baribir debug-imzoli, o'rnatsa bo'ladigan APK'ni Release sahifasiga biriktirib qo'yadi.



## Litsenziya eslatmasi
Modelning o'zi alohida litsenziyaga ega bo'lishi mumkin — distribyutsiyadan oldin tekshiring.
