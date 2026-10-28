package com.bayram.soundcorex20;

import java.util.Locale;

/**
 * Multi-lingual localization engine for Soundcore Personalized.
 * Automatically adapts to the phone's system language (TR, EN, DE, ES, FR, RU, AR, ZH)
 * and also supports instant in-app language switching.
 */
public class I18n {

    public static final String[] SUPPORTED_CODES = {"auto", "tr", "en", "de", "es", "fr", "ru", "ar"};
    public static final String[] SUPPORTED_LABELS = {
            "🌐 Auto", "🇹🇷 TR", "🇬🇧 EN", "🇩🇪 DE", "🇪🇸 ES", "🇫🇷 FR", "🇷🇺 RU", "🇸🇦 AR"
    };

    private static String overrideLang = "auto";

    public static void setLanguageOverride(String code) {
        overrideLang = (code == null || code.isEmpty()) ? "auto" : code;
    }

    public static String getLanguageOverride() {
        return overrideLang;
    }

    public static String getActiveLang() {
        if (!"auto".equals(overrideLang)) {
            return overrideLang;
        }
        String sys = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        for (String c : SUPPORTED_CODES) {
            if (c.equals(sys)) return sys;
        }
        return "en";
    }

    public static String t(String key) {
        String lang = getActiveLang();
        switch (key) {
            case "app_title":
                return "SOUNDCORE • PERSONALIZED";
            case "sync_btn":
                return pick(lang,
                        "🔄 Senkronize Et",
                        "🔄 Sync",
                        "🔄 Synchronisieren",
                        "🔄 Sincronizar",
                        "🔄 Synchroniser",
                        "🔄 Синхр.",
                        "🔄 مزامنة");
            case "grant_notif_btn":
                return pick(lang,
                        "🔓 Otomatik Medya Algılama İznini Aç (Tek Dokunuş)",
                        "🔓 Grant Smart Media Detection Permission (Tap Here)",
                        "🔓 Medien-Erkennungsberechtigung erteilen",
                        "🔓 Activar permiso de detección multimedia",
                        "🔓 Autoriser la détection média intelligente",
                        "🔓 Разрешить автоопределение медиа",
                        "🔓 تفعيل إذن اكتشاف الوسائط الذكي");
            case "left_earbud":
                return pick(lang, "SOL KULAKLIK", "LEFT EARBUD", "LINKS", "IZQUIERDO", "GAUCHE", "ЛЕВЫЙ", "السماعة اليسرى");
            case "right_earbud":
                return pick(lang, "SAĞ KULAKLIK", "RIGHT EARBUD", "RECHTS", "DERECHO", "DROITE", "ПРАВЫЙ", "السماعة اليمنى");
            case "charging_case":
                return pick(lang, "ŞARJ KUTUSU", "CHARGING CASE", "LADECASE", "ESTUCHE", "BOÎTIER", "КЕЙС", "علبة الشحن");
            case "anc_section_title":
                return pick(lang,
                        "🎧 AKTİF GÜRÜLTÜ KONTROLÜ (DONANIM DSP)",
                        "🎧 ACTIVE NOISE CONTROL (HARDWARE DSP)",
                        "🎧 AKTIVE GERÄUSCHUNTERDRÜCKUNG (DSP)",
                        "🎧 CONTROL ACTIVO DE RUIDO (DSP)",
                        "🎧 CONTRÔLE ACTIF DU BRUIT (DSP)",
                        "🎧 АКТИВНОЕ ШУМОПОДАВЛЕНИЕ (DSP)",
                        "🎧 التحكم النشط بالضوضاء (DSP)");
            case "mode_anc":
                return pick(lang,
                        "🛡️ ANC\nGürültü Engelle",
                        "🛡️ ANC\nNoise Cancel",
                        "🛡️ ANC\nGeräuschfilter",
                        "🛡️ ANC\nCancelar Ruido",
                        "🛡️ ANC\nRéduction Bruit",
                        "🛡️ ANC\nШумоподавление",
                        "🛡️ ANC\nإلغاء الضوضاء");
            case "mode_trans":
                return pick(lang,
                        "👂 Şeffaf Mod\nDış Ses Açık",
                        "👂 Transparency\nAmbient Sound",
                        "👂 Transparenz\nUmgebungston",
                        "👂 Transparencia\nSonido Ambiente",
                        "👂 Transparence\nSon Ambiant",
                        "👂 Прозрачность\nОкружающий звук",
                        "👂 وضع الشفافية\nالصوت المحيط");
            case "mode_normal":
                return pick(lang,
                        "🎵 Normal\nPasif Mod",
                        "🎵 Normal\nPassive Mode",
                        "🎵 Normal\nPassivmodus",
                        "🎵 Normal\nModo Pasivo",
                        "🎵 Normal\nMode Passif",
                        "🎵 Обычный\nПассивный",
                        "🎵 الوضع العادي\nعزل سلبي");
            case "anc_weak":
                return pick(lang, "Zayıf (1)", "Weak (1)", "Schwach (1)", "Suave (1)", "Faible (1)", "Слабый (1)", "ضعيف (1)");
            case "anc_mod":
                return pick(lang, "Orta (2)", "Moderate (2)", "Mittel (2)", "Medio (2)", "Moyen (2)", "Средний (2)", "متوسط (2)");
            case "anc_strong":
                return pick(lang, "Güçlü (3)", "Strong (3)", "Stark (3)", "Fuerte (3)", "Fort (3)", "Сильный (3)", "قوي (3)");
            case "anc_adaptive":
                return pick(lang, "Adaptif", "Adaptive", "Adaptiv", "Adaptativo", "Adaptatif", "Адаптивный", "تكيفي");
            case "wind_on":
                return pick(lang,
                        "💨 Rüzgar Gürültüsü Engelleme: AÇIK",
                        "💨 Wind Noise Suppression: ON",
                        "💨 Windgeräuschunterdrückung: EIN",
                        "💨 Reducción de Viento: ACTIVADO",
                        "💨 Réduction du Vent: ACTIVÉ",
                        "💨 Защита от ветра: ВКЛ",
                        "💨 عزل صوت الرياح: مفعل");
            case "wind_off":
                return pick(lang,
                        "💨 Rüzgar Gürültüsü Engelleme: KAPALI",
                        "💨 Wind Noise Suppression: OFF",
                        "💨 Windgeräuschunterdrückung: AUS",
                        "💨 Reducción de Viento: APAGADO",
                        "💨 Réduction du Vent: DÉSACTIVÉ",
                        "💨 Защита от ветра: ВЫКЛ",
                        "💨 عزل صوت الرياح: متوقف");
            case "trans_full":
                return pick(lang, "🌐 Tam Şeffaf Mod", "🌐 Fully Transparent", "🌐 Voll Transparent", "🌐 Transparencia Total", "🌐 Transparence Totale", "🌐 Полная прозрачность", "🌐 شفافية كاملة");
            case "trans_vocal":
                return pick(lang, "🗣️ Vokal (Net İnsan Sesi)", "🗣️ Vocal Mode (Clear Voice)", "🗣️Stimmmodus", "🗣️ Modo Vocal", "🗣️ Mode Vocal", "🗣️ Голосовой режим", "🗣️ وضع الصوت البشري");
            case "autonomous_header":
                return pick(lang,
                        "🧠 AKILLI OTONOM ADIM & BAĞLAM MOTORU",
                        "🧠 SMART AUTONOMOUS STEP & CONTEXT ENGINE",
                        "🧠 INTELLIGENTE AUTONOME SCHRITT- & KONTEXT-ENGINE",
                        "🧠 MOTOR AUTÓNOMO INTELIGENTE DE PASOS Y CONTEXTO",
                        "🧠 MOTEUR AUTONOME INTELLIGENT (PAS & CONTEXTE)",
                        "🧠 УМНЫЙ АВТОНОМНЫЙ ДВИЖОК (ШАГИ И КОНТЕКСТ)",
                        "🧠 المحرك الذكي المستقل للخطوات والسياق");
            case "card_1a_title":
                return pick(lang,
                        "1A • Akıllı Sinema & Medya Otomatik Ses",
                        "1A • Smart Cinema & Media Auto-Sound",
                        "1A • Smart-Kino & Medien Auto-Klang",
                        "1A • Sonido Automático Cine y Multimedia",
                        "1A • Son Auto Cinéma & Média Intelligent",
                        "1A • Умный звук для Кино и Медиа",
                        "1A • الصوت الذكي التلقائي للسينما والوسائط");
            case "card_1a_sub":
                return pick(lang,
                        "Ses seviyesine göre boğulmayan Sinema sahnesi (YouTube) & Dinamik Müzik EQ",
                        "Volume-adaptive Cinema warmth for YouTube & Dynamic EQ for Music without distortion",
                        "Lautstärke-adaptiver Kinoklang für YouTube & Dynamischer Musik-EQ",
                        "Sonido de cine adaptativo al volumen para YouTube y EQ dinámico",
                        "Son cinéma adaptatif au volume pour YouTube & EQ dynamique",
                        "Адаптивный кино-звук для YouTube и динамический эквалайзер",
                        "صوت سينمائي متكيف مع مستوى الصوت ليوتيوب والموسيقى");
            case "card_1b_title":
                return pick(lang,
                        "1B • Otomatik Adım Sayar & Hız Duyarlı ANC",
                        "1B • Auto Step-Counter & Speed-Aware ANC",
                        "1B • Automatischer Schrittzähler & Geschwindigkeits-ANC",
                        "1B • Podómetro Automático y ANC por Velocidad",
                        "1B • Podomètre Auto & ANC selon la Vitesse",
                        "1B • Авто-шагомер и ANC по скорости",
                        "1B • عداد الخطوات التلقائي وعزل الضوضاء حسب السرعة");
            case "card_1b_sub":
                return pick(lang,
                        "Yürümeye başladığınızda (8+ adım) otomatik Şeffaf Moda geçer, durduğunuzda ANC açar",
                        "Automatically switches to Transparency when walking (8+ steps), restores ANC when stationary",
                        "Wechselt beim Gehen (8+ Schritte) automatisch in Transparenz, bei Stillstand in ANC",
                        "Cambia a Transparencia al caminar (8+ pasos) y restaura ANC al detenerse",
                        "Passe en Transparence en marchant (8+ pas) et rétablit l'ANC à l'arrêt",
                        "Включает прозрачность при ходьбе (8+ шагов) и возвращает ANC при остановке",
                        "يتحول تلقائياً لوضع الشفافية عند المشي ويعيد ANC عند التوقف");
            case "card_1c_title":
                return pick(lang,
                        "1C • Speak-to-Chat & Akıllı Arama Asistanı",
                        "1C • Speak-to-Chat & Smart Call Assistant",
                        "1C • Speak-to-Chat & Smarter Anrufassistent",
                        "1C • Speak-to-Chat y Asistente de Llamadas",
                        "1C • Speak-to-Chat & Assistant d'Appel",
                        "1C • Speak-to-Chat и Ассистент звонков",
                        "1C • التحدث المباشر ومساعد المكالمات الذكي");
            case "card_1c_sub":
                return pick(lang,
                        "Konuştuğunuzda veya telefon çaldığında sesi kısıp Vokal Şeffaf Moda geçer",
                        "Switches to Vocal Transparency & ducks volume when you speak or during phone calls",
                        "Wechselt beim Sprechen oder bei Anrufen in den Stimm-Transparenzmodus",
                        "Cambia a Transparencia Vocal y baja el volumen al hablar o en llamadas",
                        "Passe en Transparence Vocale et baisse le volume quand vous parlez",
                        "Включает голосовой режим и приглушает музыку при разговоре или звонке",
                        "يفعل الشفافية الصوتية ويخفض الموسيقى عند التحدث أو أثناء المكالمات");
            case "btn_stationary":
                return pick(lang, "🪑 Sabit\n(Max ANC)", "🪑 Stationary\n(Max ANC)", "🪑 Sitzend\n(Max ANC)", "🪑 Quieto\n(Max ANC)", "🪑 Immobile\n(Max ANC)", "🪑 На месте\n(Макс ANC)", "🪑 ثابت\n(أقصى ANC)");
            case "btn_walking":
                return pick(lang, "🚶 Yürüyüş/Adım\n(Şeffaf Mod)", "🚶 Walking/Steps\n(Transparency)", "🚶 Gehen\n(Transparenz)", "🚶 Caminando\n(Transparencia)", "🚶 Marche\n(Transparence)", "🚶 Ходьба\n(Прозрачность)", "🚶 مشي/خطوات\n(شفافية)");
            case "btn_transit":
                return pick(lang, "🚆 Araç/Rüzgar\n(Rüzgar ANC)", "🚆 Transit/Wind\n(Wind ANC)", "🚆 Fahrzeug\n(Wind-ANC)", "🚆 Transporte\n(ANC Viento)", "🚆 Transport\n(ANC Vent)", "🚆 Транспорт\n(Ветер ANC)", "🚆 مركبة/رياح\n(عزل الرياح)");
            case "eq_section_title":
                return pick(lang,
                        "🎛️ 8-BANT DONANIM EKOLAYZIR & AKILLI SİNEMA MODU",
                        "🎛️ 8-BAND HARDWARE EQUALIZER & SMART CINEMA",
                        "🎛️ 8-BAND HARDWARE-EQUALIZER & SMART-KINO",
                        "🎛️ ECUALIZADOR HARDWARE 8 BANDAS Y MODO CINE",
                        "🎛️ ÉGALISEUR MATÉRIEL 8 BANDES & MODE CINÉMA",
                        "🎛️ 8-ПОЛОСНЫЙ ЭКВАЛАЙЗЕР И РЕЖИМ КИНО",
                        "🎛️ معادل الصوت المادي (8 نطاقات) والوضع السينمائي");
            case "cinema_boost_label":
                return pick(lang, "🎬 Sinema Derinliği ", "🎬 Cinema Depth ", "🎬 Kinotiefe ", "🎬 Modo Cine ", "🎬 Cinéma ", "🎬 Кино-бас ", "🎬 عمق سينمائي ");
            case "telemetry_title":
                return pick(lang,
                        "📡 CANLI BLUETOOTH RFCOMM & IOT TELEMETRİ KONSOLU",
                        "📡 LIVE BLUETOOTH RFCOMM & IOT TELEMETRY CONSOLE",
                        "📡 LIVE BLUETOOTH RFCOMM & IOT-TELEMETRIE",
                        "📡 CONSOLA DE TELEMETRÍA BLUETOOTH RFCOMM & IOT",
                        "📡 CONSOLE DE TÉLÉMÉTRIE BLUETOOTH RFCOMM & IOT",
                        "📡 КОНСОЛЬ ТЕЛЕМЕТРИИ BLUETOOTH RFCOMM И IOT",
                        "📡 وحدة القياس عن بعد عبر بلوتوث RFCOMM وإنترنت الأشياء");
            default:
                return key;
        }
    }

    public static String[] getPresetNames() {
        String lang = getActiveLang();
        return new String[]{
                pick(lang, "🎬 Sinema & Doğal Sahne", "🎬 Cinema & Natural Stage", "🎬 Kino & Naturklang", "🎬 Cine y Escena Natural", "🎬 Cinéma & Scène Naturelle", "🎬 Кино и Естественная сцена", "🎬 سينما وعمق طبيعي"),
                pick(lang, "🎵 Müzik + Dengeli Bass", "🎵 Music + Balanced Bass", "🎵 Musik + Bass", "🎵 Música + Bajos", "🎵 Musique + Basses", "🎵 Музыка + Бас", "🎵 موسيقى وباس متوازن"),
                pick(lang, "🎙️ Net Konuşma & Podcast", "🎙️ Clear Voice & Podcast", "🎙️ Klare Stimme & Podcast", "🎙️ Voz Clara y Podcast", "🎙️ Voix Claire & Podcast", "🎙️ Четкий голос и Подкаст", "🎙️ صوت واضح وبودكاست"),
                pick(lang, "🏃 Sport X20 Antrenman", "🏃 Sport X20 Workout", "🏃 Sport X20 Training", "🏃 Sport X20 Entrenamiento", "🏃 Sport X20 Entraînement", "🏃 Sport X20 Тренировка", "🏃 تمرين رياضي X20"),
                pick(lang, "🎸 Rock & Canlı Sahne", "🎸 Rock & Live Stage", "🎸 Rock & Live", "🎸 Rock en Vivo", "🎸 Rock & Scène", "🎸 Рок и Сцена", "🎸 روك وأداء حي"),
                pick(lang, "🎻 Saf Dengeli (0 dB)", "🎻 Pure Flat (0 dB)", "🎻 Neutral (0 dB)", "🎻 Plano Puro (0 dB)", "🎻 Neutre Pur (0 dB)", "🎻 Чистый (0 дБ)", "🎻 متوازن نقي (0 dB)")
        };
    }

    public static String pick(String lang, String tr, String en, String de, String es, String fr, String ru, String ar) {
        switch (lang) {
            case "tr": return tr;
            case "de": return de;
            case "es": return es;
            case "fr": return fr;
            case "ru": return ru;
            case "ar": return ar;
            default: return en;
        }
    }
}
