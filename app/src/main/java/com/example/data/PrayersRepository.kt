package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

class PrayersRepository(private val prayerDao: PrayerDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Ayat::class.java)
    private val adapter = moshi.adapter<List<Ayat>>(listType)

    fun toJson(list: List<Ayat>): String {
        return adapter.toJson(list)
    }

    fun fromJson(json: String): List<Ayat> {
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Expose flows and ensure initial data setup
    fun getAllPrayers(): Flow<List<PrayerItem>> {
        return prayerDao.getAllPrayers()
            .onStart {
                checkAndPrepopulateDefaults()
            }
    }

    fun getFavoritePrayers(): Flow<List<PrayerItem>> {
        return prayerDao.getFavoritePrayers()
            .onStart {
                checkAndPrepopulateDefaults()
            }
    }

    fun getPrayersByCategory(category: String): Flow<List<PrayerItem>> {
        return prayerDao.getPrayersByCategory(category)
            .onStart {
                checkAndPrepopulateDefaults()
            }
    }

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        prayerDao.updateFavoriteStatus(id, isFavorite)
    }

    val syncQueue: Flow<List<SyncQueueItem>> = prayerDao.getSyncQueue()

    suspend fun insertSyncQueueItem(category: String, title: String, ayatList: List<Ayat>) {
        val json = toJson(ayatList)
        prayerDao.insertSyncQueueItem(SyncQueueItem(category = category, title = title, ayatListJson = json))
    }

    suspend fun deleteSyncQueueItem(id: Int) {
        prayerDao.deleteSyncQueueItem(id)
    }

    suspend fun clearSyncQueue() {
        prayerDao.clearSyncQueue()
    }

    suspend fun insertCustomPrayer(category: String, title: String, ayatList: List<Ayat>) {
        val json = toJson(ayatList)
        prayerDao.insertPrayer(PrayerItem(category = category, title = title, ayatListJson = json, isCustom = true))
    }

    suspend fun deletePrayer(id: Int) {
        prayerDao.deletePrayerById(id)
    }

    suspend fun syncQueueToLocal() {
        val queue = prayerDao.getSyncQueue().first()
        if (queue.isNotEmpty()) {
            val prayers = queue.map {
                PrayerItem(
                    category = it.category,
                    title = it.title,
                    ayatListJson = it.ayatListJson,
                    isCustom = true
                )
            }
            prayerDao.insertPrayers(prayers)
            prayerDao.clearSyncQueue()
        }
    }

    private suspend fun checkAndPrepopulateDefaults() {
        val allPrayers = prayerDao.getAllPrayers().first()
        if (allPrayers.isEmpty()) {
            val defaults = getInitialDefaults()
            prayerDao.insertPrayers(defaults)
        } else {
            prayerDao.deleteDefaultPrayersByCategory("sholat")
            prayerDao.insertPrayers(getSholatDefaults())
        }
    }

    private fun getSholatDefaults(): List<PrayerItem> {
        return listOf(
            // ==========================================
            // Category: sholat (Panduan Lengkap Shalat Subuh 1-13)
            // ==========================================
            PrayerItem(
                category = "sholat",
                title = "1. Niat Shalat Subuh",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Niat", "أُصَلِّي فَرْضَ الصُّبْحِ رَكْعَتَيْنِ مُسْتَقْبِلَ الْقِبْلَةِ أَدَاءً مَأْمُومًا لِلَّهِ تَعَالَى", "Ushalli fardhash-shubhi rak'ataini mustaqbilal qiblati adaa-an ma'muman lillaahi ta'aala.", "Saya berniat shalat fardhu Subuh dua rakaat menghadap kiblat sebagai makmum karena Allah Ta'ala.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "2. Takbiratul Ihram",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Takbir", "اللَّهُ أَكْبَرُ", "Allaahu Akbar.", "Allah Maha Besar.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "3. Doa Iftitah",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Iftitah", "اللَّهُ أَكْبَرُ كَبِيرًا وَالْحَمْدُ لِلَّهِ كَثِيرًا وَسُبْحَانَ اللَّهِ بُكْرَةً وَأَصِيلًا، وَجَّهْتُ وَجْهِيَ لِلَّذِي فَطَرَ السَّمَاوَاتِ وَالْأَرْضَ حَنِيفًا مُسْلِمًا وَمَا أَنَا مِنَ الْمُشْرِكِينَ، إِنَّ صَلَاتِي وَنُسُكِي وَمَحْيَايَ وَمَمَاتِي لِلَّهِ رَبِّ الْعَالَمِينَ، لَا شَرِيكَ لَهُ وَبِذَلِكَ أُمِرْتُ وَأَنَا مِنَ الْمُسْلِمِينَ", "Allaahu akbaru kabiiran walhamdu lillaahi katsiiran, wa subhaanallaahi bukratan wa ashiila. Wajjahtu wajhiya lilladzii fatharassamawati wal ardha haniifan musliman wamaa anaa minal musyrikiin. Inna shalaatii wa nusukii wa mahyaaya wa mamaatii lillaahi rabbil 'aalamiin. Laa syariikalahu wa bidzaalika umirtu wa anaa minal muslimiin.", "Allah Maha Besar dengan segala kebesaran, segala puji bagi Allah dengan sebanyak-banyaknya puji, dan Maha Suci Allah pagi dan petang. Kuhadapkan wajahku kepada Dzat yang menciptakan langit dan bumi dalam keadaan lurus dan berserah diri dan aku bukanlah dari golongan kaum musyrikin. Sesungguhnya shalatku, ibadahku, hidupku, dan matiku hanyalah untuk Allah, Tuhan semesta alam. Tidak ada sekutu bagi-Nya dan dengan demikian aku diperintahkan dan aku termasuk orang-orang muslim.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "4. Surat Al-Fatihah",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Al-Fatihah", "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ. الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ. الرَّحْمَنِ الرَّحِيمِ. مَالِكِ يَوْمِ الدِّينِ. إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ. اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ. آمِينَ", "Bismillahirrahmanirrahim. Alhamdu lillaahi rabbil 'aalamiin. Arrahmaanirrahiim. Maaliki yawmiddiin. Iyyaaka na'budu wa iyyaaka nasta'iin. Ihdinash-shiraathal mustaqiim. Shiraathalladziina an'amta 'alaihim, ghairil maghdhuubi 'alaihim waladh-dhaalliin. Aamiin.", "Dengan menyebut nama Allah Yang Maha Pengasih lagi Maha Penyayang. Segala puji bagi Allah, Tuhan semesta alam. Yang Maha Pengasih lagi Maha Penyayang. Pemilik hari pembalasan. Hanya kepada-Mu kami menyembah dan hanya kepada-Mu kami memohon pertolongan. Tunjukkanlah kami jalan yang lurus. Yaitu jalan orang-orang yang telah Engkau beri nikmat kepada mereka, bukan jalan mereka yang dimurkai dan bukan pula jalan mereka yang sesat. Kabulkanlah ya Allah.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "5. Surat Al-Ikhlas",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Al-Ikhlas", "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ. اللَّهُ الصَّمَدُ. لَمْ يَلِدْ وَلَمْ يُولَدْ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ", "Bismillahirrahmanirrahim. Qul huwallahu ahad. Allaahush-shamad. Lam yalid walam yuulad. Walam yakullahu kufuwan ahad.", "Dengan menyebut nama Allah Yang Maha Pengasih lagi Maha Penyayang. Katakanlah (wahai Muhammad), Dialah Allah, Yang Maha Esa. Allah tempat meminta segala sesuatu. (Allah) tidak beranak dan tidak pula diperanakkan. Dan tidak ada seorang pun yang setara dengan Dia.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "6. Ruku'",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Ruku'", "سُبْحَانَ رَبِّيَ الْعَظِيمِ وَبِحَمْدِهِ (۳). اللَّهُمَّ اغْفِرْ لِي وَتُبْ عَلَيَّ، إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ", "Subhaana rabbiyal 'azhiimi wa bihamdih (3x). Allaahummaghfirlii watub 'alayya, innaka antat-tawwaabur-rahiim.", "Maha Suci Tuhanku Yang Maha Agung dengan segala puji-Nya. Ya Allah, ampunilah aku dan terimalah taubatku, sesungguhnya Engkau Maha Penerima Taubat lagi Maha Penyayang.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "7. I'tidal",
                ayatListJson = toJson(
                    listOf(
                        Ayat("I'tidal", "سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ. رَبَّنَا لَكَ الْحَمْدُ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ، مُبَارَكًا عَلَيْهِ، كَمَا يُحِبُّ رَبُّنَا وَيَرْضَى", "Sami'allaahu liman hamidah. Rabbanaa lakal hamdu hamdan katsiiran thayyiban mubaarakan fiihi, mubaarakan 'alaihi, kamaa yuhibbu Rabbunaa wa yardhaa.", "Allah mendengar orang yang memuji-Nya. Wahai Tuhan kami, bagi-Mu segala puji, pujian yang banyak, baik, dan diberkahi di dalamnya, diberkahi atasnya, sebagaimana Tuhan kami mencintai dan ridha.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "8. Sujud Pertama",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Sujud", "سُبْحَانَ رَبِّيَ الْأَعْلَى وَبِحَمْدِهِ (۳)", "Subhaana rabbiyal a'laa wa bihamdih (3x).", "Maha Suci Tuhanku Yang Maha Tinggi dengan segala puji-Nya.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "9. Duduk di Antara Dua Sujud",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Duduk", "رَبِّ اغْفِرْ لِي وَارْحَمْنِي وَاجْبُرْنِي وَارْفَعْنِي وَارْزُقْنِي وَاهْدِنِي وَعَافِنِي وَاعْفُ عَنِّي", "Rabbighfirlii warhamnii wajburnii warfa'nii warzuqnii wahdinii wa 'aafinii wa'fu 'annii.", "Ya Tuhanku, ampunilah aku, rahmatilah aku, cukupkanlah aku, angkatlah derajatku, berikanlah aku rezeki, berikanlah aku petunjuk, sehatkanlah aku, dan maafkanlah aku.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                title = "10. Sujud Kedua",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Sujud", "سُبْحَانَ رَبِّيَ الْأَعْلَى وَبِحَمْدِهِ (۳). اللَّهُمَّ إِنِّي أَسْأَلُكَ حُسْنَ الْخَاتِمَةِ اللَّهُمَّ ارْزُقْنِي تَوْبَةً نَصُوحًا قَبْلَ الْمَوْتِ اللَّهُمَّ يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ", "Subhaana rabbiyal a'laa wa bihamdih (3x). Allaahumma innii as-alukal husnal khaatimah. Allaahummarzuqnii taubatan nashuuhan qablal maut. Allaahumma yaa muqallibal quluubi tsabbit qalbii 'alaa diinik.", "Maha Suci Tuhanku Yang Maha Tinggi dengan segala puji-Nya. Ya Allah, aku memohon kepada-Mu husnul khatimah. Ya Allah, karuniakanlah kepadaku taubat yang sebenar-benarnya sebelum kematian. Ya Allah, wahai Dzat yang membolak-balikkan hati, tetapkanlah hatiku di atas agama-Mu.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                subCategory = "Sholat",
                title = "11. Doa Qunut",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Qunut", "اللَّهُمَّ اهْدِنِي فِيمَنْ هَدَيْتَ، وَعَافِنِي فِيمَنْ عَافَيْتَ، وَتَوَلَّنِي فِيمَنْ تَوَلَّيْتَ، وَبَارِكْ لِي فِيمَا أَعْطَيْتَ، وَقِنِي شَرَّ مَا قَضَيْتَ، فَإِنَّكَ تَقْضِي وَلَا يُقْضَى عَلَيْكَ، وَإِنَّهُ لَا يَذِلُّ مَنْ وَالَيْتَ، وَلَا يَعِزُّ مَنْ عَادَيْتَ، تَبَارَكْتَ رَبَّنَا وَتَعَالَيْتَ، فَلَكَ الْحَمْدُ عَلَى مَا قَضَيْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ. وَصَلَّى اللَّهُ عَلَى سَيِّدِنَا مُحَمَّدٍ النَّبِيِّ الْأُمِّيِّ وَعَلَى آلِهِ وَصَحْبِهِ وَسَلَّمَ", "Allaahumahdinii fii man hadait, wa 'aafinii fii man 'aafait, wa tawallanii fii man tawallait, wa baariklii fii maa a'thait, wa qinii syarra maa qadhait, fa innaka taqdhii wa laa yuqdhaa 'alaik, wa innahu laa yadzillu man waalait, wa laa ya'izzu man 'aadait, tabaarakta rabbanaa wa ta'aalait, falakal hamdu 'alaa maa qadhait, astaghfiruka wa atuubu ilaik. Wa shallallaahu 'alaa sayyidinaa muhammadin nabiyyil ummiyyi wa 'alaa aalihi wa shahbihi wa sallam.", "Ya Allah, berilah aku petunjuk sebagaimana orang-orang yang telah Engkau beri petunjuk, berilah aku kesehatan sebagaimana orang-orang yang telah Engkau beri kesehatan, uruslah diriku sebagaimana orang-orang yang telah Engkau urus, berkahilah untukku apa yang telah Engkau berikan kepadaku, peliharalah aku dari keburukan apa yang telah Engkau tetapkan, karena sesungguhnya Engkau-lah yang menetapkan dan tidak ada yang dapat menetapkan (ketentuan) atas-Mu, dan sesungguhnya tidak akan terhina orang yang Engkau tolong, dan tidak akan mulia orang yang Engkau musuhi. Maha Suci Engkau wahai Tuhan kami dan Maha Tinggi Engkau. Maka bagi-Mu segala puji atas apa yang telah Engkau tetapkan, aku memohon ampun kepada-Mu dan bertaubat kepada-Mu. Dan semoga Allah melimpahkan rahmat kepada junjungan kami Nabi Muhammad, Nabi yang ummi, beserta keluarga dan para sahabatnya.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                subCategory = "Sholat",
                title = "12. Tahiyat Akhir",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Tahiyat Akhir", "التَّحِيَّاتُ الْمُبَارَكَاتُ الصَّلَوَاتُ الطَّيِّبَاتُ لِلَّهِ السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللهِ وَبَرَكَاتُهُ السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ. أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِ سَيِّدِنَا مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى سَيِّدِنَا إِبْرَاهِيمَ، وَعَلَى آلِ سَيِّدِنَا إِبْرَاهِيمَ، وَبَارِكْ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِ سَيِّدِنَا مُحَمَّدٍ، كَمَا بَارَكْتَ عَلَى سَيِّدِنَا إِبْرَاهِيمَ، وَعَلَى آلِ سَيِّدِنَا إِبْرَاهِيمَ، فِي الْعَالَمِينَ إِنَّكَ حَمِيدٌ مَجِيدٌ اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ جَهَنَّمَ، وَمِنْ عَذَابِ الْقَبْرِ، وَمِنْ فِتْنَةِ الْمَحْيَا وَالْمَمَاتِ، وَمِنْ شَرِّ فِتْنَةِ الْمَسِيحِ الدَّجَّالِ", "Attahiyyaatul mubaarakatush-shalawaatuth-thayyibaatu lillaah. Assalaamu 'alaika ayyuhan-nabiyyu wa rahmatullaahi wabarakaatuh. Assalaamu 'alainaa wa 'alaa 'ibaadillaahish-shaalihiin. Asyhadu an laa ilaaha illallaah, wa asyhadu anna Muhammadar Rasuulullaah. Allaahumma shalli 'alaa sayyidinaa Muhammad, wa 'alaa aali sayyidinaa Muhammad. Kamaa shallaita 'alaa sayyidinaa Ibraahiim, wa 'alaa aali sayyidinaa Ibraahiim. Wa baarik 'alaa sayyidinaa Muhammad, wa 'alaa aali sayyidinaa Muhammad. Kamaa baarakta 'alaa sayyidinaa Ibraahiim, wa 'alaa aali sayyidinaa Ibraahiim. Fil 'aalamiina innaka hamiidum majiid. Allaahumma innii a'uudzubika min 'adzaabi jahannam, wa min 'adzaabil qabri, wa min fitnatil mahyaa wal mamaat, wa min syarri fitnatil masiihid-dajjaal.", "Segala penghormatan yang diberkahi, segala shalat yang baik adalah milik Allah. Semoga keselamatan tercurah kepadamu wahai Nabi, beserta rahmat Allah dan keberkahan-Nya. Semoga keselamatan tercurah kepada kami dan kepada hamba-hamba Allah yang shalih. Aku bersaksi bahwa tidak ada Tuhan selain Allah, dan aku bersaksi bahwa Muhammad adalah utusan Allah. Ya Allah, limpahkanlah rahmat kepada junjungan kami Nabi Muhammad, dan kepada keluarga junjungan kami Nabi Muhammad, sebagaimana Engkau telah melimpahkan rahmat kepada junjungan kami Nabi Ibrahim, dan keluarga junjungan kami Nabi Ibrahim. Dan berkahilah junjungan kami Nabi Muhammad, dan keluarga junjungan kami Nabi Muhammad, sebagaimana Engkau telah memberkahi junjungan kami Nabi Ibrahim, dan keluarga junjungan kami Nabi Ibrahim, di seluruh alam semesta, sesungguhnya Engkau Maha Terpuji lagi Maha Mulia. Ya Allah, aku berlindung kepada-Mu dari adzab neraka Jahannam, dari adzab kubur, dari fitnah kehidupan dan kematian, dan dari keburukan fitnah Al-Masih Ad-Dajjal.")
                    )
                )
            ),
            PrayerItem(
                category = "sholat",
                subCategory = "Sholat",
                title = "13. Salam",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Salam", "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ", "Assalaamu 'alaikum warahmatullaahi wabarakaatuh.", "Semoga keselamatan serta rahmat Allah dan keberkahan-Nya tercurah kepada kalian.")
                    )
                )
            )
        )
    }

    private fun getInitialDefaults(): List<PrayerItem> {
        return getSholatDefaults() + listOf(
            PrayerItem(
                category = "doa",
                subCategory = "Kesulitan",
                title = "1. Doa Kurab (Kesulitan Berat)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Doa Kurab", "لَا إِلَٰهَ إِلَّا اللهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ", "Laa ilaaha illallaahul 'Adziimul haliim, laa ilaaha illallaahu Rabbul 'arsyil 'adzhiim, laa ilaaha illallaahu Rabbus-samaawaati wa Rabbul ardhi wa Rabbul 'arsyil kariim.", "Tiada Tuhan selain Allah Yang Maha Agung lagi Maha Penyantun. Tiada Tuhan selain Allah, Tuhan Pemilik Arsy yang agung. Tiada Tuhan selain Allah, Tuhan Pemilik langit, bumi, dan Arsy yang mulia.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "2. Doa untuk Orang Tua",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Orang Tua", "رَبِّ اغْفِرْ لِي وَلِوَالِدَيَّ وَارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "Rabbighfir lii wa liwaalidayya warhamhumaa kamaa rabbayaanii shaghiiraa.", "Tuhanku, ampunilah aku dan kedua orang tuaku, dan sayangilah keduanya sebagaimana mereka mendidikku di waktu kecil.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "3. Doa Selamat / Sapu Jagat Lengkap",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Selamat", "اللَّهُمَّ إِنَّا نَسْأَلُكَ سَلَامَةً فِي الدِّينِ، وَعَافِيَةً فِي الْجَسَدِ، وَزِيَادَةً فِي الْعِلْمِ، وَبَرَكَةً فِي الرِّزْقِ، وَتَوْبَةً قَبْلَ الْمَوْتِ، وَرَحْمَةً عِنْدَ الْمَوْتِ، وَمَغْفِرَةً بَعْدَ الْمَوْتِ. اللَّهُمَّ هَوْنْ عَلَيْنَا فِي سَكَرَاتِ الْمَوْتِ، وَالنَّجَاةَ مِنَ النَّارِ، وَالْعَفْوَ عِنْدَ الْحِسَابِ", "Allahumma innaa nas-aluka salaamatan fiddiin, wa 'aafiyatan fil jasad, wa ziyaadatan fil 'ilmi, wa barakatan fir-rizqi, wa taubatan qablal maut, wa rahmatan 'indal maut, wa maghfiratan ba'dal maut. Allahumma hawwin 'alainaa fii sakaraatil maut, wan-najaata minan-naar, wal 'afwa 'indal hisaab.", "Ya Allah, kami memohon keselamatan dalam agama, kesehatan jasad, tambahan ilmu, keberkahan rezeki, taubat sebelum mati, rahmat saat mati, dan ampunan setelah mati. Ya Allah, mudahkanlah kami saat sakaratul maut, selamatkanlah dari api neraka, dan berikanlah maaf saat perhitungan amal.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "4. Doa Kebahagiaan Dunia Akhirat",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Dunia Akhirat", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Rabbana aatinaa fiddunyaa hasanah, wa fil aakhirati hasanah, wa qinaa 'adzaaban-naar.", "Tuhan kami, berikanlah kami kebaikan di dunia dan kebaikan di akhirat, dan lindungilah kami dari azab neraka.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "5. Doa Mohon Rahmat & Petunjuk (Ashabul Kahfi)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Rahmat & Petunjuk", "رَبَّنَا آتِنَا مِنْ لَدُنْكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا", "Rabbana aatinaa mil-ladunka rahmatan wa hayyi' lanaa min amrinaa rasyadaa.", "Tuhan kami, berikanlah rahmat kepada kami dari sisi-Mu dan sempurnakanlah petunjuk yang lurus bagi kami dalam urusan kami.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "6. Doa Keteguhan Iman",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Keteguhan Iman", "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً ۚ إِنَّكَ أَنْتَ الْوَهَّابُ", "Rabbana laa tuzigh quluubanaa ba'da idz hadaitanaa wa hab lanaa mil-ladunka rahmatan innaka Antal-Wahhaab.", "Tuhan kami, janganlah Engkau condongkan hati kami kepada kesesatan setelah Engkau berikan petunjuk kepada kami, dan karuniakanlah kepada kami rahmat dari sisi-Mu, sesungguhnya Engkau Maha Pemberi.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "7. Doa Agar Istiqomah Shalat & Keturunan Saleh",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Istiqomah", "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ", "Rabbijalnii muqiimas-shalaati wa min dzurriyyatii Rabbana wa taqabbal du'aa.", "Tuhanku, jadikanlah aku dan anak cucuku orang yang tetap melaksanakan shalat. Tuhan kami, terimalah doaku.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "8. Doa Ampunan Hari Kiamat",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Ampunan", "رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ", "Rabbanaghfir lii wa liwaalidayya wa lil-mu'miniina yauma yaquumul hisaab.", "Tuhan kami, ampunilah aku, kedua orang tuaku, dan orang-orang mukmin pada hari terjadinya hitungan (hari kiamat).")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "9. Doa Dimudahkan Urusan",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Urusan", "رَبِّ أُمُورًا مُتَيَسِّرَةً", "Rabbi umuuran mutayassaratan.", "Tuhanku, (berikanlah) urusan-urusan yang dimudahkan.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "10. Doa Mohon Kekuasaan/Rezeki (Nabi Sulaiman)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Kekuasaan", "رَبِّ اغْفِرْ لِي وَهَبْ لِي مُلْكًا لَا يَنْبَغِي لِأَحَدٍ مِنْ بَعْدِي ۖ إِنَّكَ أَنْتَ الْوَهَّابُ", "Rabbighfir lii wa hab lii mulkan laa yanbaghii li-ahadin mim ba'dii innaka Antal-Wahhaab.", "Tuhanku, ampunilah aku dan anugerahkanlah kepadaku kerajaan yang tidak dimiliki oleh siapa pun setelahku, sesungguhnya Engkau Maha Pemberi.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "11. Doa Kemudahan",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Kemudahan", "اللَّهُمَّ يَسِّرْ وَلَا تُعَسِّرْ", "Allahumma yassir walaa tu’assir.", "Ya Allah, mudahkanlah dan jangan Engkau persulit.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "12. Doa Pemahaman Agama",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Agama", "اللَّهُمَّ فَقِّهْنِي فِي الدِّينِ", "Allahumma faqqihni fiddiin.", "Ya Allah, pahamkanlah aku dalam urusan agama.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "13. Doa Tambah Ilmu",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Tambah Ilmu", "رَبِّ زِدْنِي عِلْمًا", "Rabbi zidnii 'ilman.", "Tuhanku, tambahkanlah ilmu kepadaku.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "14. Doa Kelapangan Hati & Kelancaran Bicara",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Kelapangan Hati", "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي يَفْقَهُوا قَوْلِي", "Rabbisy-rahlii shadrii wa yassir lii amrii wahlul 'uqdatan min lisaanii yafqahuu qawlii.", "Tuhanku, lapangkanlah dadaku, mudahkanlah urusanku, dan lepaskanlah kekakuan lidahku agar mereka mengerti perkataanku.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "15. Doa Perlindungan dari Kesedihan, Hutang & Penindasan",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Perlindungan", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَأَعُوذُ بِكَ مِنَ الْعَجْزِ وَالْكَسَلِ، وَأَعُوذُ بِكَ مِنَ الْجُبْنِ وَالْبُخْلِ، وَأَعُوذُ بِكَ مِنْ غَلَبَةِ الدَّيْنِ وَقَهْرِ الرِّجَالِ", "Allahumma inni a’udzu bika minal hammi wal hazan, wa a’udzu bika minal ‘ajzi wal kasal, wa a’udzu bika minal jubni wal bukhl, wa a’udzu bika min ghalabatid-daini wa qahrir-rijaal.", "Ya Allah, aku berlindung kepada-Mu dari kegelisahan dan kesedihan, kelemahan dan kemalasan, sifat pengecut dan kikir, serta dari lilitan hutang dan penindasan orang lain.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "16. Doa Mohon Kebaikan (Doa Nabi Musa saat Fakir)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Doa Nabi Musa", "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ", "Rabbi innii limaa anzalta ilayya min khairin faqiir.", "Tuhanku, sesungguhnya aku sangat memerlukan sesuatu kebaikan (rezeki) yang Engkau turunkan kepadaku.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "17. Doa Selamat dari Neraka",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Selamat", "اللَّهُمَّ أَجِرْنِي مِنَ النَّارِ", "Allahumma ajirnii minan-naar.", "Ya Allah, selamatkanlah aku dari api neraka.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "18. Doa Pembebasan dari Neraka",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Pembebasan", "اللَّهُمَّ أَعْتِقْ رِقَابَنَا مِنَ النَّارِ", "Allahumma a’tiq riqaabana minan-naar.", "Ya Allah, bebaskanlah leher kami (diri kami) dari api neraka.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "19. Doa Mohon Surga",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Mohon Surga", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْجَنَّةَ", "Allahumma inni as-alukal jannah.", "Ya Allah, sesungguhnya aku memohon surga kepada-Mu.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "20. Doa Hisab yang Mudah",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Hisab Mudah", "اللَّهُمَّ حَاسِبْنِي حِسَابًا يَسِيرًا", "Allahumma haasibnii hisaaban yasiiran.", "Ya Allah, periksalah aku dengan pemeriksaan yang mudah.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "21. Doa Ketakwaan & Penyucian Jiwa",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Penyucian Jiwa", "اللَّهُمَّ آتِ نَفْسِي تَقْوَاهَا، وَزَكِّهَا أَنْتَ خَيْرُ مَنْ زَكَّاهَا، أَنْتَ وَلِيُّهَا وَمَوْلَاهَا", "Allahumma aati nafsii taqwaahaa, wa zakkihaa Anta khairu man zakkaahaa, Anta waliyyuhaa wa maulaahaa.", "Ya Allah, berikanlah ketakwaan pada jiwaku dan sucikanlah ia, Engkau adalah sebaik-baik yang menyucikannya, Engkau Pemilik dan Penolongnya.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "22. Doa Lailatul Qadar / Ampunan",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Lailatul Qadar", "اللَّهُمَّ إِنَّكُ عَفُوٌّ كَرِيمٌ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", "Allahumma innaka 'afuwwun kariimun tuhibbul 'afwa fa'fu 'annii.", "Ya Allah, sesungguhnya Engkau Maha Pemaaf lagi Maha Mulia, Engkau menyukai ampunan, maka ampunilah aku.")
                    )
                )
            ),
            PrayerItem(
                category = "doa",
                title = "23. Doa Saat Ditimpa Musibah/Sakit (Nabi Ayyub)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Nabi Ayyub", "أَنِّي مَسَّنِيَ الضُّرُّ وَأَنْتَ أَرْحَمُ الرَّاحِمِينَ", "Allahumma annii massaniyad-durru wa Anta Arhamur-raahimiin.", "(Ya Allah), sesungguhnya aku telah ditimpa penyakit/kemudaratan, dan Engkau adalah Tuhan Yang Maha Penyayang di antara semua penyayang.")
                    )
                )
            ),

            // ==========================================
            // Category: dzikir (Dzikir Pagi & Sore 1-10)
            // ==========================================
            PrayerItem(
                category = "dzikir",
                title = "1. Ayat Kursi (1x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Ayat Kursi", "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَؤُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "Allahu laa ilaaha illa huwal hayyul qayyum. Laa ta'khudzuhu sinatuw walaa naum. Lahu maa fis samaawaati wa maa fil ardh. Man dzalladzii yasyfa'u 'indahu illa bi idznih. Ya'lamu maa baina aidiihim wa maa khalfahum. Walaa yuhiithuuna bi syai-im min 'ilmihii illa bimaa syaa-a. Wasi'a kursiyyuhus-samaawaati wal ardh walaa ya-uuduhu hifzhuhumaa wa huwal 'aliyyul 'azhiim.", "Allah, tidak ada tuhan selain Dia. Yang Maha Hidup, yang terus menerus mengurus (makhluk-Nya), tidak mengantuk dan tidak tidur. Milik-Nya apa yang ada di langit dan apa yang ada di bumi. Tidak ada yang dapat memberi syafaat di sisi-Nya tanpa izin-Nya. Dia mengetahui apa yang di hadapan mereka dan apa yang di belakang mereka, dan mereka tidak mengetahui sesuatu apa pun tentang ilmu-Nya melainkan apa yang Dia kehendaki. Kursi-Nya meliputi langit dan bumi. Dan Dia tidak merasa berat memelihara keduanya, dan Dia Maha Tinggi, Maha Agung.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "2. Al-Ikhlas, Al-Falaq, An-Naas (3x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Petunjuk", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ. اللَّهُ الصَّمَدُ. لَمْ يَلِدْ وَلَمْ يُولَدْ. وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ.", "Al-Ikhlas (3x) • Al-Falaq (3x) • An-Naas (3x)", "Masing-masing surat dibaca berurutan secara utuh dari Al-Ikhlas, Al-Falaq, sampai An-Naas. Ulangi urutan ini sampai 3 kali putaran penuh. Membaca ketiga surat ini sebanyak 3 kali di waktu pagi dan sore akan mencukupimu dari segala sesuatu (sebagai pelindung).")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "3. Sayyidul Istighfar (1x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Sayyidul Istighfar", "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ", "Allahumma anta rabbii laa ilaaha illa anta, khalaqtanii wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastatha'tu. A'udzubika min syarri maa shana'tu, abuu-u laka bini'matika 'alayya, wa abuu-u laka bidzanbii faghfirlii fa-innahu laa yaghfirudz-dzunuuba illa anta.", "Ya Allah, Engkau adalah Rabbku, tidak ada tuhan yang berhak disembah kecuali Engkau. Engkaulah yang menciptakanku. Aku adalah hamba-Mu. Aku akan setia pada perjanjianku dengan-Mu dan janji-Mu sesuai kemampuanku. Aku berlindung kepada-Mu dari keburukan yang kuperbuat. Aku mengakui nikmat-Mu kepadaku dan aku mengakui dosaku, oleh karena itu ampunilah aku. Sesungguhnya tiada yang dapat mengampuni dosa kecuali Engkau.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "4. Doa Masuk Waktu (1x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Pagi & Sore", "[PAGI]: اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ\n\n[SORE]: اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ", "Pagi: Allahumma bika asbahna, wa bika amsayna, wa bika nahya, wa bika namutu, wa ilaihin nusyuur.\nSore: Allahumma bika amsayna, wa bika asbahna, wa bika nahya, wa bika namutu, wa ilaikal-mashiir.", "Ya Allah, dengan rahmat dan pertolongan-Mu kami memasuki waktu pagi/sore, dan dengan rahmat dan pertolongan-Mu kami memasuki waktu sore/pagi. Dengan rahmat dan pertolongan-Mu kami hidup, dengan kehendak-Mu kami mati, dan kepada-Mu tempat kebangkitan/kembali.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "5. Dzikir Perlindungan Bahaya (3x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Perlindungan", "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", "Bismillahilladzii laa yadhurru ma'asmihi syai-un fil ardhi walaa fis-samaa-i wa huwas-samii'ul 'aliim.", "Dengan nama Allah yang bila disebut, segala sesuatu di bumi dan di langit tidak akan berbahaya, dan Dia-lah Yang Maha Mendengar lagi Maha Mengetahui.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "6. Doa 'Afiyah / Perlindungan 5 Arah (1x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Perlindungan 5 Arah", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ وَمِنْ خَلْفِي وَعَنْ يَمِينِي وَعَنْ شِمَالِي وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي", "Allahumma innii as-alukal 'afwata wal 'aafiyah fid-dunya wal aakhirah. Allahumma innii as-alukal 'afwa wal 'aafiyah fii diinii wa dunyaya wa ahlii wa maalii. Allahummastur 'auraatii wa aamin rau'aatii. Allahummahfazhnii min baini yadayya wa min khalfii wa 'an yamiinii wa 'an syimaalii wa min fauqii, wa a'uudzu bi 'azhamatika an ughtaala min tahtii.", "Ya Allah, sesungguhnya aku memohon ampunan dan keselamatan di dunia dan akhirat. Ya Allah, sesungguhnya aku memohon ampunan dan keselamatan dalam agama, dunia, keluarga, dan hartaku. Ya Allah, tutupilah auratku (aib dan celaku) dan tenteramkanlah aku dari rasa takut. Ya Allah, peliharalah aku dari depan, belakang, kanan, kiri, dan dari atasku. Dan aku berlindung dengan keagungan-Mu, agar aku tidak disergap (dibinasakan) dari bawahku.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "7. Pengakuan Keridhaan (3x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Keridhaan", "رَضِيتُ بِاللَّهِ رَبَّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيَّا", "Radhiitu billahi rabba, wa bil-islaami diina, wa bi muhammadin shallallahu 'alaihi wa sallama nabiyya.", "Aku ridha Allah sebagai Rabb, Islam sebagai agama, dan Muhammad shallallahu 'alaihi wa sallam sebagai nabi.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "8. Dzikir Khusus Waktu Sesuai Sunnah",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Khusus Waktu", "[PAGI 1x]: اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا\n\n[PAGI 3x]: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِذَادَ كَلِمَاتِهِ\n\n[SORE 3x]: أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", "Pagi: Allahumma innii as-aluka 'ilman naafi'an, wa rizqan thayyiban, wa 'amalan mutaqabbalan. / Subhanallahi wa bihamdihi, 'adada khalqihi, wa ridhaa nafsihi, wa zinata 'arsyihi, wa midaada kalimaatihi.\n\nSore: A'uudzu bikalimaatillahit-taammaati min syarri maa khalaq.", "Pagi: Mohon ilmu bermanfaat, rezeki halal, amal diterima; dan tasbih pujian seberat timbangan Arsy. Sore: Perlindungan dari kejahatan makhluk ciptaan-Nya.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "9. Penutup A: Tasbih Penghapus Dosa (100x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Tasbih", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Subhanallahi wa bihamdihi.", "Maha Suci Allah dengan segala puji bagi-Nya. Membaca tasbih ini 100 kali sehari akan menghapuskan dosa walau sebanyak buih di lautan.")
                    )
                )
            ),
            PrayerItem(
                category = "dzikir",
                title = "10. Penutup B: Tahlil Benteng Setan (100x / 10x / 1x)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Tahlil", "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Laa ilaaha illallahu wahdahu laa syariika lah, lahul mulku wa lahul hamdu wa huwa 'ala kulli syai-in qadiir.", "Tidak ada tuhan yang berhak disembah selain Allah Yang Maha Esa, tidak ada sekutu bagi-Nya. Bagi-Nya kerajaan dan bagi-Nya pujian. Dan Dia Maha Kuasa atas segala sesuatu. (Membaca 100x sebagai benteng dari setan sepanjang hari).")
                    )
                )
            ),

            // ==========================================
            // Category: sunnah (200 Sunnah Sederhana & Ringan Harian 1-12)
            // ==========================================
            PrayerItem(
                category = "sunnah",
                title = "1. Adab Bangun Tidur & Kamar Mandi",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 1", "", "", "1. Mengusap bekas tidur di wajah: Mengusap wajah dengan tangan saat pertama kali bangun."),
                        Ayat("Item 2", "", "", "2. Membaca doa bangun tidur: Membaca Alhamdulillahilladzi ahyaana..."),
                        Ayat("Item 3", "", "", "3. Mencuci kedua tangan 3 kali: Dilakukan sebelum memasukkan tangan ke wadah air."),
                        Ayat("Item 4", "", "", "4. Bersiwak / Sikat gigi: Membersihkan mulut sesaat setelah bangun tidur."),
                        Ayat("Item 5", "", "", "5. Membersihkan rongga hidung: Menghirup dan menyemburkan air (istinsyaq & istintsar) 3 kali."),
                        Ayat("Item 6", "", "", "6. Mendahulukan kaki kiri masuk WC: Melangkah dengan kaki kiri saat masuk toilet."),
                        Ayat("Item 7", "", "", "7. Membaca doa masuk WC: Memohon perlindungan dari setan jantan dan betina."),
                        Ayat("Item 8", "", "", "8. Tidak menghadap/membelakangi kiblat: Saat buang hajat di tempat terbuka/tidak berdinding."),
                        Ayat("Item 9", "", "", "9. Menggunakan tangan kiri saat istinja: Membersihkan najis dengan tangan kiri."),
                        Ayat("Item 10", "", "", "10. Tidak berbicara di dalam toilet: Diam dan tidak mengobrol saat buang hajat."),
                        Ayat("Item 11", "", "", "11. Mendahulukan kaki kanan keluar WC: Melangkah dengan kaki kanan saat keluar."),
                        Ayat("Item 12", "", "", "12. Membaca doa keluar WC: Mengucapkan \"Ghufronaka\" (Aku mohon ampunan-Mu)."),
                        Ayat("Item 13", "", "", "13. Tidak kencing berdiri tanpa udzur: Lebih utama dilakukan dengan posisi duduk/jongkok."),
                        Ayat("Item 14", "", "", "14. Menjaga kebersihan pakaian dari percikan: Berhati-hati agar urin tidak mengenai celana/badan."),
                        Ayat("Item 15", "", "", "15. Membasuh kaki dengan bersih: Memastikan sela-sela jari kaki terkena air setelah dari WC.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "2. Adab Berwudhu & Bersuci",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 16", "", "", "16. Membaca Bismillah di awal wudhu: Memulai wudhu dengan menyebut nama Allah."),
                        Ayat("Item 17", "", "", "17. Mencuci telapak tangan: Membasuh kedua telapak tangan di awal wudhu."),
                        Ayat("Item 18", "", "", "18. Berkumur-kumur secara sempurna: Memutar air di dalam mulut."),
                        Ayat("Item 19", "", "", "19. Menghirup air ke hidung: Memasukkan air ke hidung dengan tangan kanan."),
                        Ayat("Item 20", "", "", "20. Menyemburkan air dari hidung: Mengeluarkan air menggunakan tangan kiri."),
                        Ayat("Item 21", "", "", "21. Menyela-nyela jenggot yang tebal: Bagi laki-laki yang memiliki jenggot lebat."),
                        Ayat("Item 22", "", "", "22. Menyela-nyela jari tangan: Memastikan air masuk ke sela jari-jari tangan."),
                        Ayat("Item 23", "", "", "23. Menyela-nyela jari kaki: Menggunakan jari kelingking untuk menyela jari kaki."),
                        Ayat("Item 24", "", "", "24. Mengusap seluruh bagian kepala: Menjalankan tangan dari depan ke belakang lalu kembali lagi."),
                        Ayat("Item 25", "", "", "25. Mengusap bagian dalam & luar telinga: Dilakukan bersamaan setelah mengusap kepala."),
                        Ayat("Item 26", "", "", "26. Membasuh setiap anggota wudhu 3 kali: Kecuali kepala dan telinga yang diusap 1 kali."),
                        Ayat("Item 27", "", "", "27. Mendahulukan anggota tubuh bagian kanan: Membasuh tangan dan kaki kanan terlebih dahulu."),
                        Ayat("Item 28", "", "", "28. Menghemat penggunaan air wudhu: Tidak membuka keran air terlalu besar/berlebihan."),
                        Ayat("Item 29", "", "", "29. Membaca syahadat setelah wudhu: Asyhadu alla ilaha illallah wahdahu laa syarika lah..."),
                        Ayat("Item 30", "", "", "30. Membaca doa tambahan setelah wudhu: Allahummaj'alni minat tawwabina waj'alni minal mutathahhirin.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "3. Adab Berpakaian & Berhias",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 31", "", "", "31. Mendahulukan sisi kanan saat baju: Memasukkan lengan kanan terlebih dahulu."),
                        Ayat("Item 32", "", "", "32. Mendahulukan sisi kiri saat melepas baju: Mengeluarkan lengan kiri terlebih dahulu."),
                        Ayat("Item 33", "", "", "33. Mendahulukan kaki kanan saat celana: Memasukkan kaki kanan terlebih dahulu."),
                        Ayat("Item 34", "", "", "34. Mendahulukan kaki kanan saat sepatu: Memakai sandal atau sepatu sebelah kanan dulu."),
                        Ayat("Item 35", "", "", "35. Mendahulukan kaki kiri saat lepas sepatu: Melepas alas kaki sebelah kiri terlebih dahulu."),
                        Ayat("Item 36", "", "", "36. Membaca Alhamdulillah saat berpakaian: Memuji Allah atas nikmat pakaian penutup aurat."),
                        Ayat("Item 37", "", "", "37. Membaca doa memakai pakaian baru: Jika sedang berkesempatan memakai baju baru."),
                        Ayat("Item 38", "", "", "38. Memotong kuku secara berkala: Menghilangkan kotoran dan merapikan kuku."),
                        Ayat("Item 39", "", "", "39. Mencukur atau merapikan kumis: Bagi laki-laki agar penampilan bersih."),
                        Ayat("Item 40", "", "", "40. Memakai wewangian/parfum: Sangat ditekankan bagi laki-laki saat beraktivitas."),
                        Ayat("Item 41", "", "", "41. Menyisir dan merapikan rambut: Menjaga kerapian rambut kepala agar tidak acak-acakan."),
                        Ayat("Item 42", "", "", "42. Memotong rambut secara merata: Tidak memotong sebagian dan membiarkan sebagian (Qaza')."),
                        Ayat("Item 43", "", "", "43. Memakai pakaian berwarna putih: Warna pakaian yang paling disukai Rasulullah."),
                        Ayat("Item 44", "", "", "44. Memandang cermin sambil berdoa: Allahumma kamaa hassanta khalqii fa hassin khuluqii."),
                        Ayat("Item 45", "", "", "45. Membersihkan bulu ketiak: Mencabut atau mencukurnya secara rutin.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "4. Adab Keluar Rumah & Perjalanan",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 46", "", "", "46. Membaca doa keluar rumah: Bismillahi tawakkaltu 'alallah laa hawla wa laa quwwata..."),
                        Ayat("Item 47", "", "", "47. Melangkah dengan kaki kanan dulu: Saat melewati pintu keluar rumah."),
                        Ayat("Item 48", "", "", "48. Membaca doa naik kendaraan: Subhanalladzii sakhkhara lana hadza..."),
                        Ayat("Item 49", "", "", "49. Mengucapkan takbir saat jalan menanjak: Mengucapkan \"Allahu Akbar\" ketika posisi naik."),
                        Ayat("Item 50", "", "", "50. Mengucapkan tasbih saat jalan menurun: Mengucapkan \"Subhanallah\" ketika posisi turun."),
                        Ayat("Item 51", "", "", "51. Berjalan dengan tenang & wajar: Tidak berjalan terlalu cepat, lambat, atau sombong."),
                        Ayat("Item 52", "", "", "52. Menyingkirkan gangguan di jalan: Membuang paku, duri, atau batu yang menghalangi jalan."),
                        Ayat("Item 53", "", "", "53. Memberi jalan kepada orang lain: Mengalah saat berkendara atau berjalan kaki demi kelancaran."),
                        Ayat("Item 54", "", "", "54. Memilih rute jalan yang berbeda: Saat pulang dan pergi jika memungkinkan (seperti shalat Id/Jumat)."),
                        Ayat("Item 55", "", "", "55. Berdoa saat masuk pasar/mall: Membaca doa berdzikir agar dilindungi di pusat keramaian."),
                        Ayat("Item 56", "", "", "56. Menundukkan pandangan di jalan: Menjaga mata dari hal yang tidak halal dilihat."),
                        Ayat("Item 57", "", "", "57. Menjawab salam orang di jalan: Hak sesama muslim ketika berpapasan."),
                        Ayat("Item 58", "", "", "58. Menunjukkan arah bagi yang tersesat: Membantu orang yang bertanya alamat jalan."),
                        Ayat("Item 59", "", "", "59. Membaca doa saat singgah di suatu tempat: A'udzu bi kalimatillahit tammaati min syarri maa khalaq."),
                        Ayat("Item 60", "", "", "60. Segera pulang setelah urusan selesai: Tidak nongkrong sia-sia di jalan jika urusan telah kelar.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "5. Adab Makan & Minum",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 61", "", "", "61. Mencuci tangan sebelum makan: Memastikan kebersihan tangan dari kuman."),
                        Ayat("Item 62", "", "", "62. Membaca Bismillah di awal: Mengawali suapan dengan menyebut nama Allah."),
                        Ayat("Item 63", "", "", "63. Doa jika lupa membaca Bismillah: Bismillahi fii awwalihi wa aakhirihi."),
                        Ayat("Item 64", "", "", "64. Makan menggunakan tangan kanan: Wajib/sunnah yang sangat ditekankan."),
                        Ayat("Item 65", "", "", "65. Minum menggunakan tangan kanan: Tidak memegang gelas dengan tangan kiri."),
                        Ayat("Item 66", "", "", "66. Mengambil makanan yang terdekat: Tidak menjangkau lauk yang jauh jika ada yang dekat."),
                        Ayat("Item 67", "", "", "67. Makan dengan posisi duduk: Menghindari makan sambil berdiri atau berjalan."),
                        Ayat("Item 68", "", "", "68. Minum dengan posisi duduk: Sangat ditekankan agar tidak minum sambil berdiri."),
                        Ayat("Item 69", "", "", "69. Tidak meniup makanan yang panas: Membiarkannya dingin sendiri dengan dikipas ringan."),
                        Ayat("Item 70", "", "", "70. Tidak meniup minuman yang panas: Menunggu hingga layak diminum tanpa ditiup."),
                        Ayat("Item 71", "", "", "71. Minum dengan tiga kali tegukan: Diselingi bernapas di luar gelas/sedotan."),
                        Ayat("Item 72", "", "", "72. Tidak bernapas di dalam gelas: Menjauhkan mulut dari gelas saat mengambil napas."),
                        Ayat("Item 73", "", "", "73. Tidak mencela makanan yang tidak disuka: Jika suka dimakan, jika tidak suka ditinggalkan tanpa dikritik."),
                        Ayat("Item 74", "", "", "74. Memuji makanan yang dihidangkan: Menyenangkan hati orang yang memasak/menyediakan."),
                        Ayat("Item 75", "", "", "75. Makan bersama-sama dalam satu wadah: Mendatangkan keberkahan pada makanan."),
                        Ayat("Item 76", "", "", "76. Menjilat jari-jari setelah makan: Membersihkan sisa makanan di jemari sebelum dicuci."),
                        Ayat("Item 77", "", "", "77. Membersihkan sisa makanan di piring: Tidak menyisakan nasi/lauk yang terbuang mubazir."),
                        Ayat("Item 78", "", "", "78. Mengambil makanan yang jatuh ringan: Membersihkan kotorannya lalu memakannya jika layak."),
                        Ayat("Item 79", "", "", "79. Membaca Alhamdulillah setelah makan: Bentuk syukur terkecil atas rezeki makanan."),
                        Ayat("Item 80", "", "", "80. Mendoakan orang yang memberi makanan: Allahumma ath'im man ath'amanii wasqi man saqaanii.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "6. Adab Berbicara, Lisan & Bersosialisasi",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 81", "", "", "81. Berkata yang baik atau diam: Menjaga lisan dari perkataan sia-sia."),
                        Ayat("Item 82", "", "", "82. Menghindari perdebatan kusir: Mengalah dalam debat walaupun berada di pihak benar."),
                        Ayat("Item 83", "", "", "83. Tidak berbohong saat bercanda: Tetap jujur meskipun sedang bergurau."),
                        Ayat("Item 84", "", "", "84. Berbicara dengan jelas dan pelan: Agar mudah dipahami oleh pendengar."),
                        Ayat("Item 85", "", "", "85. Mengulangi perkataan penting 3 kali: Jika dirasa perlu agar poinnya benar-benar tertangkap."),
                        Ayat("Item 86", "", "", "86. Menyebarkan salam kepada sesama: Mengucapkan Assalamu'alaikum saat bertamu/bertemu."),
                        Ayat("Item 87", "", "", "87. Tersenyum di depan orang lain: Senyum bernilai sedekah paling mudah."),
                        Ayat("Item 88", "", "", "88. Berjabat tangan saat bertemu: Menggugurkan dosa-dosa kecil di antara keduanya."),
                        Ayat("Item 89", "", "", "89. Memanggil dengan nama yang baik: Tidak memanggil dengan julukan buruk/mengejek."),
                        Ayat("Item 90", "", "", "90. Tidak memotong pembicaraan orang: Mendengarkan hingga selesai baru menanggapi."),
                        Ayat("Item 91", "", "", "91. Menghindari ghibah/gosip: Tidak membicarakan keburukan orang lain."),
                        Ayat("Item 92", "", "", "92. Menahan diri dari mencaci maki: Menjaga lisan dari kata kotor dan kasar."),
                        Ayat("Item 93", "", "", "93. Mengucapkan terimakasih (Jazakallah): Bentuk apresiasi atas kebaikan orang lain."),
                        Ayat("Item 94", "", "", "94. Menghormati orang yang lebih tua: Memberikan posisi atau prioritas bicara pada mereka."),
                        Ayat("Item 95", "", "", "95. Menyayangi orang yang lebih muda: Berbicara dengan lembut dan penuh kasih kepada anak-anak."),
                        Ayat("Item 96", "", "", "96. Mengucapkan Alhamdulillah saat bersin: Adab reflek setelah bersin."),
                        Ayat("Item 97", "", "", "97. Mendoakan orang bersin yang memuji Allah: Mengucapkan \"Yarhamukallah\"."),
                        Ayat("Item 98", "", "", "98. Membalas doa bersin: Mengucapkan \"Yahdikumullah wa yushlihu baalakum\"."),
                        Ayat("Item 99", "", "", "99. Menahan menguap sebisa mungkin: Merapatkan bibir agar tidak terbuka lebar."),
                        Ayat("Item 100", "", "", "100. Menutup mulut dengan tangan saat menguap: Menggunakan tangan kiri untuk menutup mulut.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "7. Adab Bertamu & Bertetangga",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 101", "", "", "101. Meminta izin/ketuk pintu maksimal 3 kali: Jika tidak direspons setelah 3 kali, hendaknya pulang."),
                        Ayat("Item 102", "", "", "102. Tidak berdiri lurus menghadap pintu: Berdiri di samping kanan/kiri pintu saat mengetuk."),
                        Ayat("Item 103", "", "", "103. Menyebutkan nama jelas saat ditanya: Tidak menjawab \"saya\" atau \"aku\" saat tuan rumah bertanya."),
                        Ayat("Item 104", "", "", "104. Mengucapkan salam sebelum masuk: Ucapkan salam saat pintu dibukakan."),
                        Ayat("Item 105", "", "", "105. Menerima suguhan dengan senang hati: Menghargai apa yang disajikan oleh tuan rumah."),
                        Ayat("Item 106", "", "", "106. Tidak melirik-lirik isi rumah bertamu: Menjaga pandangan tetap sopan di ruang tamu."),
                        Ayat("Item 107", "", "", "107. Duduk di tempat yang disediakan: Mengikuti arahan posisi duduk dari tuan rumah."),
                        Ayat("Item 108", "", "", "108. Saling memberi hadiah dengan tetangga: Saling bertukar makanan atau oleh-oleh kecil."),
                        Ayat("Item 109", "", "", "109. Memperbanyak kuah masakan: Agar bisa dibagikan sebagian ke tetangga terdekat."),
                        Ayat("Item 110", "", "", "110. Tidak mengganggu ketenangan tetangga: Menjaga volume suara/musik agar tidak bising."),
                        Ayat("Item 111", "", "", "111. Menjenguk tetangga yang sakit: Bentuk empati dan perhatian sosial."),
                        Ayat("Item 112", "", "", "112. Membantu tetangga yang kesulitan: Meminjamkan alat atau bantuan tenaga."),
                        Ayat("Item 113", "", "", "113. Memulai salam saat bertemu tetangga: Menjadi orang pertama yang menyapa."),
                        Ayat("Item 114", "", "", "114. Menghormati hak-hak tetangga non-muslim: Tetap berbuat baik dan adil dalam urusan sosial."),
                        Ayat("Item 115", "", "", "115. Mengantar jenazah tetangga jika wafat: Ikut menyolatkan atau mengiringi ke makam.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "8. Adab Jual Beli & Muamalah Harian",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 116", "", "", "116. Bersikap ramah saat menjual: Melayani pembeli dengan senyuman dan jujur."),
                        Ayat("Item 117", "", "", "117. Bersikap toleran saat membeli: Tidak menawar harga terlalu sadis/kejam."),
                        Ayat("Item 118", "", "", "118. Memudahkan saat menagih utang: Berbicara sopan dan memberikan tempo jika ia kesulitan."),
                        Ayat("Item 119", "", "", "119. Mencatat utang piutang: Menulis nominal dan tanggal agar tidak lupa."),
                        Ayat("Item 120", "", "", "120. Berniat segera membayar utang: Tidak menunda-nunda pembayaran jika sudah ada uang."),
                        Ayat("Item 121", "", "", "121. Memberikan kembalian dengan benar: Tidak mengurangi hak pembeli walau sedikit."),
                        Ayat("Item 122", "", "", "122. Menjelaskan cacat barang jualan: Jujur terhadap kondisi barang yang ditawarkan."),
                        Ayat("Item 123", "", "", "123. Tidak bersumpah palsu demi laris: Menghindari kalimat \"Demi Allah barang ini modalnya mahal\"."),
                        Ayat("Item 124", "", "", "124. Memberikan timbangan yang adil: Pas dan tidak mencurangi alat ukur/timbangan."),
                        Ayat("Item 125", "", "", "125. Mengucapkan terimakasih setelah transaksi: Saling mendoakan keberkahan rezeki.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "9. Adab di Dalam Rumah & Keluarga",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 126", "", "", "126. Mengucapkan salam masuk rumah kosong: Assalamu'alaika wa 'ala 'ibadillahis shalihin."),
                        Ayat("Item 127", "", "", "127. Membaca Bismillah saat menutup pintu: Dilakukan di waktu malam hari."),
                        Ayat("Item 128", "", "", "128. Menutup wadah makanan di malam hari: Sambil menyebut nama Allah (membaca Bismillah)."),
                        Ayat("Item 129", "", "", "129. Menutup wadah air/minum malam hari: Mengikat atau menutup permukaannya."),
                        Ayat("Item 130", "", "", "130. Mematikan lampu/sumber api sebelum tidur: Menjaga keamanan rumah dari kebakaran."),
                        Ayat("Item 131", "", "", "131. Membantu pekerjaan rumah tangga: Ikut menyapu, mencuci piring, atau merapikan rumah."),
                        Ayat("Item 132", "", "", "132. Memanggil keluarga dengan nama kesayangan: Panggilan lembut untuk menciptakan kasih sayang."),
                        Ayat("Item 133", "", "", "133. Mencium anak kecil/keponakan: Bentuk kasih sayang kepada generasi muda."),
                        Ayat("Item 134", "", "", "134. Menahan anak keluar rumah saat Maghrib: Mengondisikan anak di dalam rumah saat awal malam."),
                        Ayat("Item 135", "", "", "135. Memberi makan hewan peliharaan: Berbuat baik kepada kucing, burung, dll."),
                        Ayat("Item 136", "", "", "136. Tidak membiarkan piring kotor menumpuk: Segera membersihkan wadah setelah digunakan."),
                        Ayat("Item 137", "", "", "137. Membuang sampah pada tempatnya: Menjaga kebersihan area dalam rumah."),
                        Ayat("Item 138", "", "", "138. Menyapa orang tua dengan takzim: Berbicara lembut dan sopan di depan ibu/ayah."),
                        Ayat("Item 139", "", "", "139. Makan bersama seluruh anggota keluarga: Berkumpul di satu meja/tikar makan."),
                        Ayat("Item 140", "", "", "140. Menasihati keluarga dengan cara privat: Tidak menegur kesalahan di depan banyak orang.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "10. Adab Menuju Tidur & Malam Hari",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 141", "", "", "141. Berwudhu sebelum tidur: Tidur dalam keadaan suci dari hadats."),
                        Ayat("Item 142", "", "", "142. Mengibas tempat tidur 3 kali: Membersihkan kasur dengan kain sebelum direbahkan."),
                        Ayat("Item 143", "", "", "143. Berbaring miring ke sisi kanan: Posisi awal rebahan yang dianjurkan."),
                        Ayat("Item 144", "", "", "144. Meletakkan tangan kanan di bawah pipi: Telapak tangan kanan menopang pipi kanan."),
                        Ayat("Item 145", "", "", "145. Membaca Ayat Kursi sebelum tidur: Sebagai pelindung diri dari gangguan setan."),
                        Ayat("Item 146", "", "", "146. Membaca Al-Ikhlas, Al-Falaq, An-Naas: Ditiupkan ke telapak tangan lalu diusap ke badan."),
                        Ayat("Item 147", "", "", "147. Membaca doa tidur pendek: Bismika Allahumma ahya wa bismika amut."),
                        Ayat("Item 148", "", "", "148. Memaafkan kesalahan orang lain: Membersihkan hati dari dendam sebelum tidur."),
                        Ayat("Item 149", "", "", "149. Berniat bangun untuk shalat Subuh: Menata niat kuat di dalam hati."),
                        Ayat("Item 150", "", "", "150. Tidak begadang untuk hal tidak bermanfaat: Segera tidur setelah shalat Isya jika tidak ada urusan.")
                    )
                )
            ),
            PrayerItem(
                category = "sunnah",
                title = "11. Dzikir, Doa Sederhana & Rutinitas Ringan Harian",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Item 151", "", "", "151. Membaca Istighfar 3x setelah shalat fardhu: Mengucapkan Astaghfirullah."),
                        Ayat("Item 152", "", "", "152. Membaca Allahumma antas salam...: Dzikir rutin setelah shalat fardhu."),
                        Ayat("Item 196", "", "", "196. Membaca doa kafaratul majlis: Subhanakallahumma wa bihamdika asyhadu alla ilaha...")
                    )
                )
            ),
            PrayerItem(
                category = "qiyamul",
                title = "Sayyidul Istighfar",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Bagian 1", "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ خَلَقْتَنِي وَأَنَا عَبْدُكَ", "Allahumma anta rabbii laa ilaaha illaa anta, khalaqtanii wa ana 'abduka", "Ya Allah, Engkau adalah Tuhanku, tidak ada Tuhan selain Engkau. Engkau yang menciptakan aku dan aku adalah hamba-Mu,"),
                        Ayat("Bagian 2", "وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ", "wa ana 'alaa 'ahdika wa wa'dika mastatha'tu", "dan aku di atas perjanjian-Mu dan janji-Mu sekadar kemampuanku."),
                        Ayat("Bagian 3", "أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ", "A'idzu bika min syarri maa shana'tu, abuu-u laka bini'matika 'alayya", "Aku berlindung kepada-Mu dari keburukan apa yang kuperbuat, aku mengakui nikmat-Mu kepadaku"),
                        Ayat("Bagian 4", "وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ", "wa abuu-u bi dzanbii, faghfirlii fa-innahu laa yaghfirudz dzuunuuba illaa anta", "dan aku mengakui dosaku, maka ampunilah aku karena sesungguhnya tidak ada yang mengampuni dosa selain Engkau.")
                    )
                )
            ),
            PrayerItem(
                category = "qiyamul",
                title = "Doa Pengakuan & Cahaya (Allahumma Lakal Hamdu)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Bagian 1", "اللَّهُمَّ لَكَ الْحَمْدُ أَنْتَ نُورُ السَّمَاوَاتِ وَالْأَرْضِ وَمَنْ فِيهِنَّ", "Allahumma lakal hamdu anta nuurus samawaati wal ardhi wa man fiihinna", "Ya Allah, bagi-Mu segala puji, Engkau cahaya langit dan bumi serta apa yang ada di dalamnya."),
                        Ayat("Bagian 2", "وَلَكَ الْحَمْدُ أَنْتَ قَيِّمُ السَّمَاوَاتِ وَالْأَرْضِ وَمَنْ فِيهِنَّ", "Wa lakal hamdu anta qayyimus samawaati wal ardhi wa man fiihinna", "Bagi-Mu segala puji, Engkau pengatur langit dan bumi serta apa yang ada di dalamnya."),
                        Ayat("Bagian 3", "وَلَكَ الْحَمْدُ أَنْتَ رَبُّ السَّمَاوَاتِ وَالْأَرْضِ وَمَنْ فِيهِنَّ", "Wa lakal hamdu anta rabbus samawaati wal ardhi wa man fiihinna", "Bagi-Mu segala puji, Engkau Tuhan langit dan bumi serta apa yang ada di dalamnya."),
                        Ayat("Bagian 4", "وَلَكَ الْحَمْدُ أَنْتَ الْحَقُّ وَوَعْدُكَ حَقٌّ وَلِقَاؤُكَ حَقٌّ وَقَوْلُكَ حَقٌّ", "Wa lakal hamdu anta haqqun, wa wa'duka haqqun, wa liqaa-uka haqqun, wa qauluka haqqun", "Bagi-Mu segala puji, Engkau Maha Benar, janji-Mu benar, pertemuan dengan-Mu benar, dan firman-Mu benar."),
                        Ayat("Bagian 5", "وَالْجَنَّةُ حَقٌّ وَالنَّارُ حَقٌّ وَالنَّبِيُّونَ حَقٌّ", "wal jannatu haqqun, wan naaru haqqun, wan nabiyyuuna haqqun", "Surga itu benar, neraka itu benar, dan para nabi itu benar,"),
                        Ayat("Bagian 6", "وَمُحَمَّدٌ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ حَقٌّ وَالسَّاعَةُ حَقٌّ", "wa Muhammadun shallallahu 'alaihi wa sallama haqqun, was saa'atu haqqun", "dan Muhammad SAW itu benar, dan hari kiamat itu benar."),
                        Ayat("Bagian 7", "اللَّهُمَّ لَكَ أَسْلَمْتُ وَبِكَ آمَنْتُ وَعَلَيْكَ تَوَكَّلْتُ وَإِلَيْكَ أَنَبْتُ", "Allahumma laka aslamtu, wa bika aamantu, wa 'alaika tawakkaltu, wa ilaika anabtu", "Ya Allah, kepada-Mu aku berserah diri, kepada-Mu aku beriman, kepada-Mu aku bertawakal, dan kepada-Mu aku kembali,"),
                        Ayat("Bagian 8", "وَبِكَ خَاصَمْتُ وَإِلَيْكَ حَاكَمْتُ فَاغْفِرْ لِي مَا قَدَّمْتُ وَمَا أَخَّرْتُ", "wa bika khaashamtu, wa ilaika haakamtu, faghfirlii maa qaddamtu wa maa akh-khartu", "kepada-Mu aku mengadu, dan kepada-Mu aku berhukum, maka ampunilah dosaku yang telah lalu dan yang akan datang,"),
                        Ayat("Bagian 9", "وَمَا أَسْرَرْتُ وَمَا أَعْلَنْتُ أَنْتَ الْمُقَدِّمُ وَأَنْتَ الْمُؤَخِّرُ لَا إِلَٰهَ إِلَّا أَنْتَ", "wa maa asrartu wa maa a'lantu, antal muqaddimu wa antal mu-akh-khiru, laa ilaaha illaa anta", "yang kurahasiakan dan yang kunyatakan, Engkau Yang Mendahului dan Engkau Yang Mengakhiri, tidak ada Tuhan selain Engkau.")
                    )
                )
            ),
            PrayerItem(
                category = "qiyamul",
                title = "Doa Inti Rasulullah (Mohon Perlindungan)",
                ayatListJson = toJson(
                    listOf(
                        Ayat("Bagian 1", "اللَّهُمَّ إِنِّي أَعُوذُ بِرِضَاكَ مِنْ سَخَطِكَ وَبِمُعَافَاتِكَ مِنْ عُقُوبَتِكَ", "Allahumma inni a'udzu bi ridhooka min sakhothika, wa bi mu'aafaatika min 'uquubatika", "Ya Allah, sesungguhnya aku berlindung dengan keridhaan-Mu dari kemurkaan-Mu, dengan keselamatan-Mu dari hukuman-Mu,"),
                        Ayat("Bagian 2", "وَأَعُوذُ بِكَ مِنْكَ لَا أُحْصِي ثَنَاءً عَلَيْكَ أَنْتَ كَمَا أَثْنَيْتَ عَلَىٰ نَفْسِكَ", "wa a'udzu bika minka, laa uh-shii tsanaa-an 'alaika, anta kamaa atsnaita 'alaa nafsik", "dan aku berlindung kepada-Mu dari siksa-Mu. Aku tidak mampu menghitung pujian atas-Mu, Engkau adalah sebagaimana Engkau memuji diri-Mu sendiri.")
                    )
                )
            )
        )
    }
}
