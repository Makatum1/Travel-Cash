package com.example.ui.screens

import com.example.ui.theme.AppLanguage

data class FaqLocaleText(
    val category: String,
    val question: String,
    val answer: String
)

object FaqTranslations {
    private val translations: Map<Pair<String, AppLanguage>, FaqLocaleText> = mapOf(
        // ---------------- FR (Français) ----------------
        ("faq_create_trip" to AppLanguage.FR) to FaqLocaleText(
            category = "Gestion des voyages",
            question = "Comment créer un nouveau voyage et de quelles données ai-je besoin ?",
            answer = "Accédez à l'onglet « Voyages » dans la barre inférieure et appuyez sur « + Nouveau voyage ». Saisissez le nom, les dates de début et de fin, le budget estimé (facultatif), la devise de base (EUR, USD...) et les participants. Une fois enregistré, il devient votre voyage actif pour enregistrer vos dépenses."
        ),
        ("faq_switch_trip" to AppLanguage.FR) to FaqLocaleText(
            category = "Gestion des voyages",
            question = "Comment changer de voyage actif ou consulter mes voyages précédents ?",
            answer = "Dans l'onglet « Voyages », le voyage avec le badge « ACTIF » est celui affiché dans les onglets Dépenses, Données et Comptes. Pour changer de voyage, appuyez simplement sur sa carte pour l'activer."
        ),
        ("faq_edit_delete_trip" to AppLanguage.FR) to FaqLocaleText(
            category = "Gestion des voyages",
            question = "Comment modifier ou supprimer un voyage existant ?",
            answer = "Dans l'onglet « Voyages », appuyez sur l'icône de crayon pour modifier le titre, les dates, le budget ou les participants. Pour le supprimer, appuyez sur la corbeille (confirmation requise)."
        ),
        ("faq_trip_header_photo" to AppLanguage.FR) to FaqLocaleText(
            category = "Gestion des voyages",
            question = "Comment changer la photo ou l'icône d'en-tête d'un voyage ?",
            answer = "Sur la carte d'en-tête du voyage actif, appuyez sur l'icône ronde de route. Vous pouvez prendre une photo avec l'appareil photo, choisir dans votre galerie ou restaurer l'icône par défaut."
        ),
        ("faq_common_pot" to AppLanguage.FR) to FaqLocaleText(
            category = "Cagnotte commune",
            question = "Comment fonctionne la cagnotte commune et qu'est-ce que le « Solde restant » ?",
            answer = "La cagnotte est un fonds collectif pour régler les dépenses partagées (taxis, repas, entrées...). La carte « SOLDE RESTANT » indique le montant net disponible dans la cagnotte. En vert si positif, en rouge en cas de déficit nécessitant un réapprovisionnement."
        ),
        ("faq_golden_rule" to AppLanguage.FR) to FaqLocaleText(
            category = "Cagnotte commune",
            question = "Quelle est la « Règle d'or » lors d'un apport par personne ?",
            answer = "En sélectionnant « Montant par personne » dans l'apport à la cagnotte, l'application déduit automatiquement ce qui est dû aux participants ayant avancé des frais de leur poche. Celui qui a avancé paie moins ou rien, équilibrant les comptes sans virements intermédiaires."
        ),
        ("faq_pot_vs_personal" to AppLanguage.FR) to FaqLocaleText(
            category = "Cagnotte commune",
            question = "Quand marquer une dépense payée par la « Cagnotte » ou par un participant ?",
            answer = "Choisissez « 🏺 Bote Común » (Cagnotte) si l'argent provient de la caisse commune. Choisissez le nom du participant s'il a payé de sa poche avec son argent ou sa carte personnelle."
        ),
        ("faq_leftover_pot" to AppLanguage.FR) to FaqLocaleText(
            category = "Cagnotte commune",
            question = "Que devient l'argent restant dans la cagnotte à la fin du voyage ?",
            answer = "Si le solde restant est positif, dans l'onglet « Comptes », l'application calcule exactement le remboursement dû à chaque voyageur selon ses apports et sa consommation réelle."
        ),
        ("faq_add_expense" to AppLanguage.FR) to FaqLocaleText(
            category = "Dépenses",
            question = "Comment ajouter une dépense à un voyage ?",
            answer = "Dans l'onglet « Dépenses », appuyez sur le bouton flottant « + ». Renseignez la description, le montant, la date, la catégorie, le payeur et cochez les participants concernés."
        ),
        ("faq_split_expenses" to AppLanguage.FR) to FaqLocaleText(
            category = "Dépenses",
            question = "Comment diviser une dépense entre les participants ? (Division des dépenses)",
            answer = "Lors de la saisie d'une dépense, tous les participants sont cochés par défaut pour un partage équitable. Décochez ceux qui n'ont pas participé pour répartir le montant uniquement entre les membres cochés."
        ),
        ("faq_order_by_date" to AppLanguage.FR) to FaqLocaleText(
            category = "Dépenses",
            question = "Comment consulter ou trier les dépenses par date dans un voyage ?",
            answer = "Dans l'onglet « Dépenses », les dépenses sont groupées et triées par jour. Utilisez la barre de recherche ou les puces de filtres pour isoler des catégories ou personnes."
        ),
        ("faq_attach_photo" to AppLanguage.FR) to FaqLocaleText(
            category = "Dépenses",
            question = "Comment joindre une photo ou un reçu à une dépense ?",
            answer = "Lors de l'ajout ou modification d'une dépense, appuyez sur l'icône appareil photo ou galerie pour joindre le reçu du ticket de caisse."
        ),
        ("faq_gps_location" to AppLanguage.FR) to FaqLocaleText(
            category = "Dépenses",
            question = "Comment fonctionnent la géolocalisation GPS et les suggestions de commerces ?",
            answer = "En tapant le nom d'un lieu avec la permission de localisation active, Travel Cash utilise OpenStreetMap pour suggérer les commerces proches et placer la dépense sur l'onglet « Carte »."
        ),
        ("faq_splits_calculation" to AppLanguage.FR) to FaqLocaleText(
            category = "Comptes et balances",
            question = "Comment l'application calcule-t-elle les dettes et les soldes dans « Comptes » ?",
            answer = "Pour chaque voyageur, l'application calcule le total payé (de sa poche + apports à la cagnotte) moins le total consommé. Le solde indique si on lui doit de l'argent (+) ou s'il en doit (-)."
        ),
        ("faq_optimized_settlements" to AppLanguage.FR) to FaqLocaleText(
            category = "Comptes et balances",
            question = "Que sont les « Règlements de dettes optimisés » ?",
            answer = "L'algorithme de Travel Cash simplifie toutes les dettes du groupe au nombre minimal de virements possibles, permettant de solder tous les comptes en quelques paiements directs."
        ),
        ("faq_change_currency" to AppLanguage.FR) to FaqLocaleText(
            category = "Devises et budget",
            question = "Comment changer de devise ou de format monétaire ?",
            answer = "La devise principale se définit lors de la création ou modification du voyage. Lors de la saisie d'une dépense, vous pouvez choisir une devise étrangère et son taux de change."
        ),
        ("faq_budget_alerts" to AppLanguage.FR) to FaqLocaleText(
            category = "Devises et budget",
            question = "Comment fonctionnent le budget et la barre de progression des dépenses ?",
            answer = "La barre de progression affiche le pourcentage du budget consommé. Si vous dépassez 90 % de la limite, elle passe au rouge pour alerter le groupe."
        ),
        ("faq_export_zip" to AppLanguage.FR) to FaqLocaleText(
            category = "Exportation et sauvegarde",
            question = "Comment exporter un voyage en ZIP ?",
            answer = "Rendez-vous dans « Paramètres », bloc « Exporter et Importer », et appuyez sur « Exporter ». Le fichier ZIP s'ouvre avec la feuille de partage native Android."
        ),
        ("faq_export_pdf" to AppLanguage.FR) to FaqLocaleText(
            category = "Exportation et sauvegarde",
            question = "Comment générer le rapport PDF d'un voyage ?",
            answer = "Dans l'onglet « Voyages », appuyez sur « Exporter en PDF ». Travel Cash génère un rapport financier complet et ouvre le sélecteur d'applications pour le partager."
        ),
        ("faq_backup_restore" to AppLanguage.FR) to FaqLocaleText(
            category = "Exportation et sauvegarde",
            question = "Comment sauvegarder ou restaurer un voyage ?",
            answer = "Exportez le fichier .zip ou .json et conservez-le. Pour restaurer vos données sur un nouvel appareil, utilisez le bouton « Importer »."
        ),
        ("faq_share_sheet" to AppLanguage.FR) to FaqLocaleText(
            category = "Exportation et sauvegarde",
            question = "Comment partager directement via WhatsApp, e-mail ou Google Drive ?",
            answer = "Chaque export (PDF, CSV, JSON, ZIP) ouvre la feuille de partage native d'Android (Share Sheet) vous permettant de choisir librement l'application destinataire."
        ),
        ("faq_change_language" to AppLanguage.FR) to FaqLocaleText(
            category = "Paramètres et personnalisation",
            question = "Comment changer la langue de l'application ?",
            answer = "Dans l'onglet « Paramètres », choisissez votre langue parmi les 8 langues disponibles (Espagnol, Anglais, Français, Italien, Chinois, Néerlandais, Allemand, Polonais). L'interface bascule immédiatement."
        ),
        ("faq_daily_notifications" to AppLanguage.FR) to FaqLocaleText(
            category = "Paramètres et personnalisation",
            question = "Comment activer le rappel quotidien de saisie des dépenses ?",
            answer = "Dans « Paramètres », activez la carte « Rappel quotidien de dépenses » et choisissez l'heure souhaitée pour recevoir une notification chaque soir."
        ),

        // ---------------- IT (Italiano) ----------------
        ("faq_create_trip" to AppLanguage.IT) to FaqLocaleText(
            category = "Gestione viaggi",
            question = "Come creo un nuovo viaggio e quali dati servono?",
            answer = "Vai alla scheda «Viaggi» e premi «+ Nuovo viaggio». Inserisci titolo, date di inizio e fine, budget stimato (opzionale), valuta base e partecipanti. Diventerà il tuo viaggio attivo per registrare le spese."
        ),
        ("faq_switch_trip" to AppLanguage.IT) to FaqLocaleText(
            category = "Gestione viaggi",
            question = "Come cambio il viaggio attivo o consulto i viaggi precedenti?",
            answer = "Nella scheda «Viaggi», il viaggio contrassegnato con «ATTIVO» è quello visualizzato nelle schede Spese, Dati e Saldi. Tocca la scheda di un altro viaggio per attivarlo."
        ),
        ("faq_edit_delete_trip" to AppLanguage.IT) to FaqLocaleText(
            category = "Gestione viaggi",
            question = "Come modifico o elimino un viaggio esistente?",
            answer = "Nella scheda «Viaggi», tocca l'icona della matita per modificare i dettagli o il cestino per eliminarlo insieme alle sue spese."
        ),
        ("faq_trip_header_photo" to AppLanguage.IT) to FaqLocaleText(
            category = "Gestione viaggi",
            question = "Come cambio la foto o l'icona di copertina di un viaggio?",
            answer = "Sulla scheda superiore del viaggio attivo, tocca l'icona rotonda dell'itinerario. Puoi scattare una foto, sceglierne una dalla galleria o ripristinare l'icona predefinita."
        ),
        ("faq_common_pot" to AppLanguage.IT) to FaqLocaleText(
            category = "Cassa comune",
            question = "Come funziona la cassa comune e cos'è il «Saldo extra»?",
            answer = "La cassa comune è un fondo per acquisti condivisi (taxi, pasti, ingressi...). La scheda «SALDO EXTRA» mostra il denaro netto disponibile. Verde se positivo, rosso in caso di deficit da ricaricare."
        ),
        ("faq_golden_rule" to AppLanguage.IT) to FaqLocaleText(
            category = "Cassa comune",
            question = "Cos'è la «Regola d'oro» quando si versa quota per persona?",
            answer = "Scegliendo la modalità «Quota per persona», l'app sottrae automaticamente quanto anticipato da chi ha pagato spese di tasca propria. Chi ha anticipato paga meno o nulla, saldando i debiti senza bonifici."
        ),
        ("faq_pot_vs_personal" to AppLanguage.IT) to FaqLocaleText(
            category = "Cassa comune",
            question = "Quando indicare una spesa pagata con «Cassa» o da un partecipante?",
            answer = "Seleziona «🏺 Bote Común» (Cassa comune) se il denaro proviene dalla cassa del gruppo; seleziona il nome della persona se ha pagato con i propri soldi o la propria carta."
        ),
        ("faq_leftover_pot" to AppLanguage.IT) to FaqLocaleText(
            category = "Cassa comune",
            question = "Cosa succede se avanzano soldi nella cassa comune a fine viaggio?",
            answer = "Se il saldo è positivo, nella scheda «Saldi» l'app calcola esattamente l'importo da restituire a ciascun partecipante in proporzione ai versamenti e consumi effettivi."
        ),
        ("faq_add_expense" to AppLanguage.IT) to FaqLocaleText(
            category = "Spese",
            question = "Come aggiungo una spesa a un viaggio?",
            answer = "Nella scheda «Spese», tocca il pulsante fluttuante «+». Inserisci descrizione, importo, data, categoria, chi ha pagato e seleziona i partecipanti beneficiari."
        ),
        ("faq_split_expenses" to AppLanguage.IT) to FaqLocaleText(
            category = "Spese",
            question = "Come si dividono le spese tra i partecipanti? (Divisione spese)",
            answer = "Tutti i partecipanti sono selezionati per impostazione predefinita per una divisione equa. Deseleziona chi non ha usufruito della spesa per ripartire l'importo solo tra i rimanenti."
        ),
        ("faq_order_by_date" to AppLanguage.IT) to FaqLocaleText(
            category = "Spese",
            question = "Come consulto o ordino le spese per data in un viaggio?",
            answer = "Nella scheda «Spese», le spese sono ordinate cronologicamente per giorno. Usa la barra di ricerca o i filtri per categoria e partecipante."
        ),
        ("faq_attach_photo" to AppLanguage.IT) to FaqLocaleText(
            category = "Spese",
            question = "Come allego una foto o uno scontrino a una spesa?",
            answer = "Durante la creazione o modifica di una spesa, tocca il pulsante fotocamera o galleria per salvare l'immagine dello scontrino."
        ),
        ("faq_gps_location" to AppLanguage.IT) to FaqLocaleText(
            category = "Spese",
            question = "Come funzionano la geolocalizzazione GPS e i suggerimenti dei luoghi?",
            answer = "Digitando il nome di un locale con il GPS attivo, Travel Cash suggerisce i luoghi vicini tramite OpenStreetMap e inserisce la spesa nella scheda «Mappa»."
        ),
        ("faq_splits_calculation" to AppLanguage.IT) to FaqLocaleText(
            category = "Saldi e bilancio",
            question = "Come calcola l'app i debiti e i saldi nella scheda «Saldi»?",
            answer = "Per ciascun partecipante si calcola il totale pagato (di tasca propria + versamenti in cassa) meno il totale consumato. La differenza indica se deve ricevere (+) o dare (-) denaro."
        ),
        ("faq_optimized_settlements" to AppLanguage.IT) to FaqLocaleText(
            category = "Saldi e bilancio",
            question = "Cosa sono i «Pagamenti di liquidazione ottimizzati»?",
            answer = "L'algoritmo di Travel Cash riduce tutti i debiti incrociati al numero minimo indispensabile di trasferimenti diretti, chiudendo tutti i conti con pochi pagamenti."
        ),
        ("faq_change_currency" to AppLanguage.IT) to FaqLocaleText(
            category = "Valute e budget",
            question = "Come cambio valuta o formato monetario?",
            answer = "La valuta principale si imposta creando o modificando il viaggio. Per le singole spese puoi selezionare una valuta estera e specificare il tasso di cambio."
        ),
        ("faq_budget_alerts" to AppLanguage.IT) to FaqLocaleText(
            category = "Valute e budget",
            question = "Come funzionano il budget e la barra di avanzamento delle spese?",
            answer = "La barra mostra la percentuale di budget spesa in tempo reale. Se supera il 90 %, diventa rossa per avvisare che si è vicini al limite."
        ),
        ("faq_export_zip" to AppLanguage.IT) to FaqLocaleText(
            category = "Esportazione e backup",
            question = "Come esporto un viaggio in ZIP?",
            answer = "In «Impostazioni», sezione «Esporta e Importa», premi «Esporta». Il file compresso .zip si apre con il menu di condivisione di Android."
        ),
        ("faq_export_pdf" to AppLanguage.IT) to FaqLocaleText(
            category = "Esportazione e backup",
            question = "Come genero il riepilogo PDF di un viaggio?",
            answer = "Nella scheda «Viaggi», premi «Esporta in PDF». Travel Cash genera un report completo multipagina e apre il selettore di condivisione."
        ),
        ("faq_backup_restore" to AppLanguage.IT) to FaqLocaleText(
            category = "Esportazione e backup",
            question = "Come faccio un backup o ripristino un viaggio?",
            answer = "Esporta il file .zip o .json e salvalo su Drive o email. Quando cambi dispositivo, premi «Importa» per recuperare tutti i dati."
        ),
        ("faq_share_sheet" to AppLanguage.IT) to FaqLocaleText(
            category = "Esportazione e backup",
            question = "Come condivido direttamente via WhatsApp, email o Google Drive?",
            answer = "Ogni esportazione apre il menu nativo di condivisione di Android (Share Sheet) per inviare o salvare direttamente con l'app desiderata."
        ),
        ("faq_change_language" to AppLanguage.IT) to FaqLocaleText(
            category = "Impostazioni e personalizzazione",
            question = "Come cambio la lingua dell'applicazione?",
            answer = "Nella scheda «Impostazioni», scegli la tua lingua preferita tra le 8 supportate. L'intera app si aggiornerà istantaneamente."
        ),
        ("faq_daily_notifications" to AppLanguage.IT) to FaqLocaleText(
            category = "Impostazioni e personalizzazione",
            question = "Come attivo il promemoria giornaliero delle spese?",
            answer = "In «Impostazioni», attiva «Promemoria giornaliero spese» e imposta l'orario desiderato per ricevere un avviso serale."
        ),

        // ---------------- ZH (中文简体) ----------------
        ("faq_create_trip" to AppLanguage.ZH) to FaqLocaleText(
            category = "行程管理",
            question = "如何创建新行程？需要填写哪些信息？",
            answer = "点击底部导航栏的“行程”标签，然后点击“+ 新建行程”。输入行程名称、起止日期、预算上限（选填）、本币币种（如 EUR、USD、CNY 等）以及初始成员，保存后即可成为当前激活行程开始记账。"
        ),
        ("faq_switch_trip" to AppLanguage.ZH) to FaqLocaleText(
            category = "行程管理",
            question = "如何切换当前行程或查看以往行程？",
            answer = "在“行程”页面中，标有“当前激活”徽章的行程是当前在支出、统计和结算中显示的行程。点击其他行程卡片即可快速切换。"
        ),
        ("faq_edit_delete_trip" to AppLanguage.ZH) to FaqLocaleText(
            category = "行程管理",
            question = "如何修改或删除已有行程？",
            answer = "在“行程”页面中，点击铅笔图标可修改名称、日期、预算或成员；点击垃圾桶图标可在二次确认后删除该行程及所有相关记账数据。"
        ),
        ("faq_trip_header_photo" to AppLanguage.ZH) to FaqLocaleText(
            category = "行程管理",
            question = "如何更换行程封面图标或照片？",
            answer = "在顶部卡片或行程卡片上点击路线圆形图标，支持拍照上传、从相册选取或恢复默认路线图标。"
        ),
        ("faq_common_pot" to AppLanguage.ZH) to FaqLocaleText(
            category = "公账与分摊",
            question = "公账（共同金）如何运作？什么是“公账余额”？",
            answer = "公账是大家为共同开销（打车、聚餐、门票）筹集的资金池。顶部卡片的“公账余额”实时展示可用资金：绿色为结余，红色为透支（提示需要补缴）。"
        ),
        ("faq_golden_rule" to AppLanguage.ZH) to FaqLocaleText(
            category = "公账与分摊",
            question = "按人头存入公账时的“黄金法则”是什么？",
            answer = "选择“按人头存入”模式时，系统会自动应用财务黄金法则：若某成员之前已垫付过团体开销，系统会自动从其应存份额中扣减，无需私下转账即可完成抵扣与对账。"
        ),
        ("faq_pot_vs_personal" to AppLanguage.ZH) to FaqLocaleText(
            category = "公账与分摊",
            question = "何时选择由“公账支付”或个人支付？",
            answer = "如果钱是从团体公账现金或公共卡中支出的，付款人选择“🏺 公账”；如果是由某位成员自掏腰包垫付的，则选择该成员的名字，以便系统计算应报销金额。"
        ),
        ("faq_leftover_pot" to AppLanguage.ZH) to FaqLocaleText(
            category = "公账与分摊",
            question = "旅行结束公账有结余该如何处理？",
            answer = "在“结算”页面中，系统会根据每位成员的净存入与实际消费金额，精确计算出每个人应退还的金额，确保公平。"
        ),
        ("faq_add_expense" to AppLanguage.ZH) to FaqLocaleText(
            category = "支出记账",
            question = "如何在行程中添加一笔支出？",
            answer = "在“支出”页面点击右下角悬浮按钮“+”，输入消费明细、金额、日期、分类，选择付款人并勾选分摊该笔支出的成员即可。"
        ),
        ("faq_split_expenses" to AppLanguage.ZH) to FaqLocaleText(
            category = "支出记账",
            question = "如何分摊支出或均摊费用？（费用分摊）",
            answer = "默认会勾选所有成员进行均摊。如果某位成员未参与该项消费（如没吃某顿饭），只需取消勾选其名字，费用将仅在勾选成员间均摊。"
        ),
        ("faq_order_by_date" to AppLanguage.ZH) to FaqLocaleText(
            category = "支出记账",
            question = "如何按日期查看或排序行程支出？",
            answer = "支出页面会自动按天归类并按时间倒序排列。您可以使用顶部搜索栏或分类筛选标签快速检索。"
        ),
        ("faq_attach_photo" to AppLanguage.ZH) to FaqLocaleText(
            category = "支出记账",
            question = "如何为支出附加小票或单据照片？",
            answer = "添加或编辑支出时，点击相机或相册图标即可为开销附上发票小票照片，点击支出卡片即可全屏查看高清图。"
        ),
        ("faq_gps_location" to AppLanguage.ZH) to FaqLocaleText(
            category = "支出记账",
            question = "GPS 定位与商户自动联想是如何工作的？",
            answer = "开启定位权限后，输入商户名称时 Travel Cash 会实时通过 OpenStreetMap 检索周围商户并自动填充地址，同时在“地图”中标记该支出。"
        ),
        ("faq_splits_calculation" to AppLanguage.ZH) to FaqLocaleText(
            category = "结算与对账",
            question = "“结算”页面是如何计算债务和差额的？",
            answer = "系统为每人计算：1. 总支付额（个人垫付 + 存入公账）；2. 总消费额（参与均摊的金额）。两者差额即为净余额：正数为应收款，负数为应付款。"
        ),
        ("faq_optimized_settlements" to AppLanguage.ZH) to FaqLocaleText(
            category = "结算与对账",
            question = "什么是“最简化转账结算”？",
            answer = "Travel Cash 算法将复杂的群组债务简化为最少笔数的直连转账（如“张三转账 50 元给李四”），无需互相多次转账即可清账。"
        ),
        ("faq_change_currency" to AppLanguage.ZH) to FaqLocaleText(
            category = "货币与预算",
            question = "如何修改货币或货币显示格式？",
            answer = "在创建或编辑行程时设定基准货币。记录单笔支出时也可以选择外币并输入汇率进行折算。"
        ),
        ("faq_budget_alerts" to AppLanguage.ZH) to FaqLocaleText(
            category = "货币与预算",
            question = "预算进度条是如何工作的？",
            answer = "进度条实时展示当前支出占预算的百分比。当超过 90% 时会自动变红以提醒超支风险。"
        ),
        ("faq_export_zip" to AppLanguage.ZH) to FaqLocaleText(
            category = "导出与备份",
            question = "如何将行程导出为 ZIP 压缩包？",
            answer = "在“设置”页面的“导出与导入”中点击“导出”，生成的 .zip 压缩包将通过 Android 原生分享面板发送或保存。"
        ),
        ("faq_export_pdf" to AppLanguage.ZH) to FaqLocaleText(
            category = "导出与备份",
            question = "如何生成行程 PDF 财务报告？",
            answer = "在“行程”页面点击“导出 PDF”，系统会自动生成包含财务指标、图表、成员明细和清账方案的 PDF 文件并调用分享。"
        ),
        ("faq_backup_restore" to AppLanguage.ZH) to FaqLocaleText(
            category = "导出与备份",
            question = "如何备份或恢复行程数据？",
            answer = "导出 .zip 或 .json 文件保存至网盘。更换手机或重装应用后，点击“导入”即可完整恢复所有行程、开销和成员数据。"
        ),
        ("faq_share_sheet" to AppLanguage.ZH) to FaqLocaleText(
            category = "导出与备份",
            question = "如何直接分享文件到微信、邮件或网盘？",
            answer = "导出任何文件（PDF、CSV、JSON、ZIP）时，系统都会直接调出 Android 系统原生分享菜单，供您自由选择接收应用。"
        ),
        ("faq_change_language" to AppLanguage.ZH) to FaqLocaleText(
            category = "设置与个性化",
            question = "如何更改应用语言？",
            answer = "在“设置”的语言选项中选择您需要的语言（支持中文、英语、西班牙语、法语、意大利语、荷兰语、德语、波兰语 8 种语言），界面实时切换。"
        ),
        ("faq_daily_notifications" to AppLanguage.ZH) to FaqLocaleText(
            category = "设置与个性化",
            question = "如何开启每日记账提醒通知？",
            answer = "在“设置”中开启“每日记账提醒”开关，并设置期望的时间（例如 21:00），系统会在每晚提醒您记录当天开销。"
        ),

        // ---------------- NL (Nederlands) ----------------
        ("faq_create_trip" to AppLanguage.NL) to FaqLocaleText(
            category = "Reisbeheer",
            question = "Hoe maak ik een nieuwe reis aan en welke gegevens heb ik nodig?",
            answer = "Ga naar het tabblad «Reizen» en tik op «+ Nieuwe reis». Vul de bestemming, begin- en einddatum, geschat budget (optioneel), basisvaluta en deelnemers in."
        ),
        ("faq_switch_trip" to AppLanguage.NL) to FaqLocaleText(
            category = "Reisbeheer",
            question = "Hoe wissel ik van actieve reis of bekijk ik eerdere reizen?",
            answer = "In het tabblad «Reizen» toont de badge «ACTIEF» de huidige reis. Tik op een andere reis om deze meteen te activeren."
        ),
        ("faq_edit_delete_trip" to AppLanguage.NL) to FaqLocaleText(
            category = "Reisbeheer",
            question = "Hoe bewerk of verwijder ik een bestaande reis?",
            answer = "Tik in «Reizen» op het potloodicoon om gegevens aan te passen, of op het prullenbakicoon om de reis na bevestiging te verwijderen."
        ),
        ("faq_trip_header_photo" to AppLanguage.NL) to FaqLocaleText(
            category = "Reisbeheer",
            question = "Hoe wijzig ik de headerfoto of het route-icoon van een reis?",
            answer = "Tik op de bovenste kaart op het ronde route-icoon. Je kunt een foto maken met de camera, kiezen uit de galerij of het standaardicoon herstellen."
        ),
        ("faq_common_pot" to AppLanguage.NL) to FaqLocaleText(
            category = "Gezamenlijke pot",
            question = "Hoe werkt de gezamenlijke pot en wat is «Extra saldo»?",
            answer = "De gezamenlijke pot is een groepspot voor gedeelde uitgaven (taxi, eten, tickets...). De kaart «EXTRA SALDO» toont het beschikbare bedrag (groen bij overschot, rood bij tekort)."
        ),
        ("faq_golden_rule" to AppLanguage.NL) to FaqLocaleText(
            category = "Gezamenlijke pot",
            question = "Wat is de «Gouden Regel» bij inleg per persoon in de pot?",
            answer = "Bij «Bedrag per persoon» trekt de app automatisch voorgeschoten bedragen af van de inleg van die deelnemer, waardoor schulden zonder tussenliggende overboekingen worden vereffend."
        ),
        ("faq_pot_vs_personal" to AppLanguage.NL) to FaqLocaleText(
            category = "Gezamenlijke pot",
            question = "Wanneer kies ik voor betaling uit de «Pot» of door een persoon?",
            answer = "Kies «🏺 Bote Común» (Pot) als het geld uit de groepspot kwam; kies de naam van de deelnemer als deze uit eigen zak heeft betaald."
        ),
        ("faq_leftover_pot" to AppLanguage.NL) to FaqLocaleText(
            category = "Gezamenlijke pot",
            question = "Wat gebeurt er als er geld overblijft in de pot na afloop?",
            answer = "In het tabblad «Splitsen» berekent de app exact hoeveel geld aan iedere deelnemer moet worden terugbetaald naar rato van inleg en verbruik."
        ),
        ("faq_add_expense" to AppLanguage.NL) to FaqLocaleText(
            category = "Uitgaven",
            question = "Hoe voeg ik een uitgave toe aan een reis?",
            answer = "Tik op «+» in het tabblad «Uitgaven». Voer omschrijving, bedrag, datum en categorie in, geef aan wie heeft betaald en selecteer de deelnemers."
        ),
        ("faq_split_expenses" to AppLanguage.NL) to FaqLocaleText(
            category = "Uitgaven",
            question = "Hoe splits ik een uitgave onder deelnemers? (Uitgaven splitsen)",
            answer = "Standaard is iedereen aangevinkt voor gelijke verdeling. Vink deelnemers uit die niet meededen om het bedrag alleen over de overigen te verdelen."
        ),
        ("faq_order_by_date" to AppLanguage.NL) to FaqLocaleText(
            category = "Uitgaven",
            question = "Hoe bekijk of sorteer ik uitgaven op datum?",
            answer = "Uitgaven worden automatisch per dag gegroepeerd en gesorteerd. Gebruik het zoekveld en de filters om categorieën of personen te filteren."
        ),
        ("faq_attach_photo" to AppLanguage.NL) to FaqLocaleText(
            category = "Uitgaven",
            question = "Hoe voeg ik een foto of kassabon toe aan een uitgave?",
            answer = "Tik bij het toevoegen of bewerken van een uitgave op het camera- of galerij-icoon om een foto van de bon op te slaan."
        ),
        ("faq_gps_location" to AppLanguage.NL) to FaqLocaleText(
            category = "Uitgaven",
            question = "Hoe werken gps-locatie en automatische winkelsuggesties?",
            answer = "Bij het typen van een locatie zoekt Travel Cash via OpenStreetMap naar nabijgelegen zaken en plaatst de uitgave op de «Kaart»."
        ),
        ("faq_splits_calculation" to AppLanguage.NL) to FaqLocaleText(
            category = "Splitsen en saldo",
            question = "Hoe berekent de app schulden en saldi in «Splitsen»?",
            answer = "De app berekent per reiziger het totaal betaalde bedrag min het verbruikte aandeel. Het saldo geeft aan of iemand geld tegoed heeft (+) of verschuldigd is (-)."
        ),
        ("faq_optimized_settlements" to AppLanguage.NL) to FaqLocaleText(
            category = "Splitsen en saldo",
            question = "Wat zijn «Geoptimaliseerde vereffeningen»?",
            answer = "Het algoritme van Travel Cash brengt alle onderlinge schulden terug naar het minimale aantal directe overboekingen om iedereen in één keer af te lossen."
        ),
        ("faq_change_currency" to AppLanguage.NL) to FaqLocaleText(
            category = "Valuta en budget",
            question = "Hoe wijzig ik valuta of valutanotatie?",
            answer = "De basisvaluta kies je bij het aanmaken van de reis. Bij individuele uitgaven kun je een vreemde valuta en wisselkoers invoeren."
        ),
        ("faq_budget_alerts" to AppLanguage.NL) to FaqLocaleText(
            category = "Valuta en budget",
            question = "Hoe werken het budget en de voortgangsbalk van uitgaven?",
            answer = "De voortgangsbalk toont het percentage besteed budget. Boven 90 % kleurt de balk rood als waarschuwing."
        ),
        ("faq_export_zip" to AppLanguage.NL) to FaqLocaleText(
            category = "Exporteren en back-up",
            question = "Hoe exporteer ik een reis naar ZIP?",
            answer = "Ga in «Instellingen» naar «Exporteren en Importeren» en tik op «Exporteren». Het ZIP-bestand opent via het Android-deelmenu."
        ),
        ("faq_export_pdf" to AppLanguage.NL) to FaqLocaleText(
            category = "Exporteren en back-up",
            question = "Hoe genereer ik een PDF-rapport van een reis?",
            answer = "Tik in «Reizen» op «Exporteren naar PDF». Travel Cash genereert een compleet financieel rapport en opent het deelvenster."
        ),
        ("faq_backup_restore" to AppLanguage.NL) to FaqLocaleText(
            category = "Exporteren en back-up",
            question = "Hoe maak ik een back-up of herstel ik een reis?",
            answer = "Exporteer het .zip- of .json-bestand naar Google Drive of e-mail. Gebruik op een nieuw toestel «Importeren» om alles te herstellen."
        ),
        ("faq_share_sheet" to AppLanguage.NL) to FaqLocaleText(
            category = "Exporteren en back-up",
            question = "Hoe deel ik bestanden via WhatsApp, e-mail of Google Drive?",
            answer = "Elke exportoptie opent direct het standaard Android-deelvenster (Share Sheet) zodat je zelf je favoriete app kiest."
        ),
        ("faq_change_language" to AppLanguage.NL) to FaqLocaleText(
            category = "Instellingen en aanpassing",
            question = "Hoe wijzig ik de taal van de app?",
            answer = "Ga naar «Instellingen» en kies je taal uit de 8 opties (Spaans, Engels, Frans, Italiaans, Chinees, Nederlands, Duits, Pools)."
        ),
        ("faq_daily_notifications" to AppLanguage.NL) to FaqLocaleText(
            category = "Instellingen en aanpassing",
            question = "Hoe activeer ik dagelijkse herinneringen voor uitgaven?",
            answer = "Schakel in «Instellingen» de «Dagelijkse herinnering» in en stel de gewenste tijd in om 's avonds een notificatie te ontvangen."
        ),

        // ---------------- DE (Deutsch) ----------------
        ("faq_create_trip" to AppLanguage.DE) to FaqLocaleText(
            category = "Reiseverwaltung",
            question = "Wie erstelle ich eine neue Reise und welche Angaben brauche ich?",
            answer = "Gehen Sie zum Reiter «Reisen» und tippen Sie auf «+ Neue Reise». Geben Sie Name, Start- und Enddatum, geschätztes Budget (optional), Basiswährung und Teilnehmer ein."
        ),
        ("faq_switch_trip" to AppLanguage.DE) to FaqLocaleText(
            category = "Reiseverwaltung",
            question = "Wie wechsle ich die aktive Reise oder sehe frühere Reisen ein?",
            answer = "Im Reiter «Reisen» zeigt die Kennzeichnung «AKTIV» die aktuelle Reise an. Tippen Sie auf eine andere Reisekarte, um sie zu aktivieren."
        ),
        ("faq_edit_delete_trip" to AppLanguage.DE) to FaqLocaleText(
            category = "Reiseverwaltung",
            question = "Wie bearbeite oder lösche ich eine bestehende Reise?",
            answer = "Tippen Sie auf das Stiftsymbol zum Bearbeiten oder auf das Papierkorbsymbol, um die Reise nach Bestätigung zu löschen."
        ),
        ("faq_trip_header_photo" to AppLanguage.DE) to FaqLocaleText(
            category = "Reiseverwaltung",
            question = "Wie ändere ich das Titelbild oder Routensymbol einer Reise?",
            answer = "Tippen Sie auf der Kopfkarte auf das runde Symbol. Sie können ein Foto aufnehmen, ein Bild aus der Galerie wählen oder das Standardsymbol wiederherstellen."
        ),
        ("faq_common_pot" to AppLanguage.DE) to FaqLocaleText(
            category = "Gemeinschaftskasse",
            question = "Wie funktioniert die Gemeinschaftskasse und was ist das «Extraguthaben»?",
            answer = "Die Gemeinschaftskasse ist ein gemeinsamer Topf für Gruppenkäufe (Taxi, Essen, Tickets...). Die Karte «EXTRAGUTHABEN» zeigt den verfügbaren Betrag (grün bei Plus, rot bei Fehlbetrag)."
        ),
        ("faq_golden_rule" to AppLanguage.DE) to FaqLocaleText(
            category = "Gemeinschaftskasse",
            question = "Was ist die «Goldene Regel» bei Einzahlungen pro Person?",
            answer = "Im Modus «Betrag pro Person» zieht das System automatisch verauslagte Beträge von der Einzahlung der betreffenden Person ab, sodass Schulden ohne Überweisungen ausgeglichen werden."
        ),
        ("faq_pot_vs_personal" to AppLanguage.DE) to FaqLocaleText(
            category = "Gemeinschaftskasse",
            question = "Wann wähle ich Bezahlung aus der «Kasse» oder durch eine Person?",
            answer = "Wählen Sie «🏺 Bote Común» (Kasse), wenn das Geld aus dem Gemeinschaftstopf kam; wählen Sie den Namen des Teilnehmers, wenn er aus eigener Tasche bezahlt hat."
        ),
        ("faq_leftover_pot" to AppLanguage.DE) to FaqLocaleText(
            category = "Gemeinschaftskasse",
            question = "Was passiert mit Restgeld in der Kasse nach Reiseende?",
            answer = "Im Reiter «Abrechnung» berechnet die App genau, welcher Betrag jedem Teilnehmer entsprechend seiner Einzahlungen und seines tatsächlichen Verbrauchs zusteht."
        ),
        ("faq_add_expense" to AppLanguage.DE) to FaqLocaleText(
            category = "Ausgaben",
            question = "Wie füge ich einer Reise eine Ausgabe hinzu?",
            answer = "Tippen Sie im Reiter «Ausgaben» auf «+». Tragen Sie Beschreibung, Betrag, Datum und Kategorie ein, wählen Sie den Zahler und markieren Sie die Begünstigten."
        ),
        ("faq_split_expenses" to AppLanguage.DE) to FaqLocaleText(
            category = "Ausgaben",
            question = "Wie teile ich Ausgaben unter Teilnehmern auf? (Ausgabenaufteilung)",
            answer = "Standardmäßig sind alle für eine gleichmäßige Aufteilung markiert. Deaktivieren Sie Personen, die nicht beteiligt waren, um den Betrag nur auf die Übrigen umzulegen."
        ),
        ("faq_order_by_date" to AppLanguage.DE) to FaqLocaleText(
            category = "Ausgaben",
            question = "Wie sortiere oder filtere ich Ausgaben nach Datum?",
            answer = "Ausgaben werden automatisch nach Tagen gruppiert und sortiert. Nutzen Sie das Suchfeld und Filterchips für Kategorien oder Personen."
        ),
        ("faq_attach_photo" to AppLanguage.DE) to FaqLocaleText(
            category = "Ausgaben",
            question = "Wie hänge ich einen Beleg oder ein Foto an eine Ausgabe an?",
            answer = "Tippen Sie beim Erstellen oder Bearbeiten einer Ausgabe auf das Kamera- oder Galeriesymbol, um ein Foto des Kassenbons zu speichern."
        ),
        ("faq_gps_location" to AppLanguage.DE) to FaqLocaleText(
            category = "Ausgaben",
            question = "Wie funktionieren GPS-Ortung und automatische Ortsvorschläge?",
            answer = "Bei aktivierter Standortberechtigung schlägt Travel Cash über OpenStreetMap nahegelegene Orte vor und markiert die Ausgabe im Reiter «Karte»."
        ),
        ("faq_splits_calculation" to AppLanguage.DE) to FaqLocaleText(
            category = "Abrechnung und Salden",
            question = "Wie berechnet die App Schulden und Salden in «Abrechnung»?",
            answer = "Pro Person wird die Gesamteinzahlung (eigene Auslagen + Einzahlungen in die Kasse) minus der Gesamtverbrauch berechnet. Der Saldo zeigt Guthaben (+) oder Verbindlichkeit (-)."
        ),
        ("faq_optimized_settlements" to AppLanguage.DE) to FaqLocaleText(
            category = "Abrechnung und Salden",
            question = "Was sind «Optimierte Ausgleichszahlungen»?",
            answer = "Der Algorithmus von Travel Cash fasst alle Verflechtungen auf die geringstmögliche Anzahl direkter Überweisungen zusammen, um alle Schulden schnell zu begleichen."
        ),
        ("faq_change_currency" to AppLanguage.DE) to FaqLocaleText(
            category = "Währungen & Budget",
            question = "Wie ändere ich die Währung oder das Format?",
            answer = "Die Basiswährung wird beim Erstellen der Reise festgelegt. Bei Einzelausgaben können Sie eine Fremdwährung und den Wechselkurs erfassen."
        ),
        ("faq_budget_alerts" to AppLanguage.DE) to FaqLocaleText(
            category = "Währungen & Budget",
            question = "Wie funktionieren das Budget und der Fortschrittsbalken?",
            answer = "Der Balken zeigt den Anteil des verbrauchten Budgets. Über 90 % verfärbt er sich rot, um vor einer Überschreitung zu warnen."
        ),
        ("faq_export_zip" to AppLanguage.DE) to FaqLocaleText(
            category = "Export & Datensicherung",
            question = "Wie exportiere ich eine Reise als ZIP?",
            answer = "Gehen Sie in «Einstellungen» zu «Exportieren und Importieren» und tippen Sie auf «Exportieren». Die ZIP-Datei öffnet das Android-Teilen-Menü."
        ),
        ("faq_export_pdf" to AppLanguage.DE) to FaqLocaleText(
            category = "Export & Datensicherung",
            question = "Wie erstelle ich einen PDF-Bericht einer Reise?",
            answer = "Tippen Sie in «Reisen» auf «Als PDF exportieren». Travel Cash erstellt einen vollständigen Finanzbericht und öffnet die Teilen-Auswahl."
        ),
        ("faq_backup_restore" to AppLanguage.DE) to FaqLocaleText(
            category = "Export & Datensicherung",
            question = "Wie erstelle ich ein Backup oder stelle eine Reise wieder her?",
            answer = "Exportieren Sie die .zip- oder .json-Datei auf Google Drive oder per E-Mail. Nutzen Sie auf einem neuen Gerät «Importieren», um alles wiederherzustellen."
        ),
        ("faq_share_sheet" to AppLanguage.DE) to FaqLocaleText(
            category = "Export & Datensicherung",
            question = "Wie teile ich Dateien über WhatsApp, E-Mail oder Google Drive?",
            answer = "Jeder Export öffnet direkt das native Android-Teilen-Menü (Share Sheet), sodass Sie die gewünschte Ziel-App frei wählen können."
        ),
        ("faq_change_language" to AppLanguage.DE) to FaqLocaleText(
            category = "Einstellungen & Personalisierung",
            question = "Wie ändere ich die App-Sprache?",
            answer = "Wählen Sie in den «Einstellungen» Ihre bevorzugte Sprache aus den 8 unterstützten Optionen (Spanisch, Englisch, Französisch, Italienisch, Chinesisch, Niederländisch, Deutsch, Polnisch)."
        ),
        ("faq_daily_notifications" to AppLanguage.DE) to FaqLocaleText(
            category = "Einstellungen & Personalisierung",
            question = "Wie aktiviere ich tägliche Ausgabenerinnerungen?",
            answer = "Aktivieren Sie in «Einstellungen» die «Tägliche Erinnerung» und stellen Sie die gewünschte Uhrzeit ein."
        ),

        // ---------------- PL (Polski) ----------------
        ("faq_create_trip" to AppLanguage.PL) to FaqLocaleText(
            category = "Zarządzanie podróżami",
            question = "Jak utworzyć nową podróż i jakich danych potrzebuję?",
            answer = "Przejdź do zakładki «Podróże» i naciśnij «+ Nowa podróż». Wprowadź nazwę, daty rozpoczęcia i zakończenia, szacowany budżet (opcjonalnie), walutę bazową i uczestników."
        ),
        ("faq_switch_trip" to AppLanguage.PL) to FaqLocaleText(
            category = "Zarządzanie podróżami",
            question = "Jak zmienić aktywną podróż lub przejrzeć poprzednie podróże?",
            answer = "W zakładce «Podróże» etykieta «AKTYWNA» wskazuje bieżącą podróż. Dotknij innej karty podróży, aby natychmiast ją aktywować."
        ),
        ("faq_edit_delete_trip" to AppLanguage.PL) to FaqLocaleText(
            category = "Zarządzanie podróżami",
            question = "Jak edytować lub usunąć istniejącą podróż?",
            answer = "W zakładce «Podróże» dotknij ikony ołówka, aby edytować szczegóły, lub kosza, aby usunąć podróż wraz z powiązanymi wydatkami."
        ),
        ("faq_trip_header_photo" to AppLanguage.PL) to FaqLocaleText(
            category = "Zarządzanie podróżami",
            question = "Jak zmienić zdjęcie w nagłówku lub ikonę trasy podróży?",
            answer = "Dotknij okrągłej ikony trasy na górnej karcie podróży. Możesz zrobić zdjęcie aparatem, wybrać z galerii lub przywrócić domyślną ikonę."
        ),
        ("faq_common_pot" to AppLanguage.PL) to FaqLocaleText(
            category = "Wspólna kasa",
            question = "Jak działa wspólna kasa i czym jest «Dodatkowe saldo»?",
            answer = "Wspólna kasa to fundusz na wspólne zakupy (taksówki, posiłki, bilety...). Karta «DODATKOWE SALDO» pokazuje dostępne środki (zielony przy nadwyżce, czerwony przy deficycie)."
        ),
        ("faq_golden_rule" to AppLanguage.PL) to FaqLocaleText(
            category = "Wspólna kasa",
            question = "Czym jest «Złota Zasada» przy wpłatach na osobę?",
            answer = "W trybie «Kwota na osobę» aplikacja automatycznie odlicza kwoty wyłożone z własnej kieszeni od składki danej osoby, rozliczając długi bez dodatkowych przelewów."
        ),
        ("faq_pot_vs_personal" to AppLanguage.PL) to FaqLocaleText(
            category = "Wspólna kasa",
            question = "Kiedy oznaczyć wydatek jako opłacony ze «Wspólnej kasy» a kiedy przez osobę?",
            answer = "Wybierz «🏺 Bote Común» (Wspólna kasa), jeśli zapłacono ze wspólnych pieniędzy; wybierz nazwisko uczestnika, jeśli wyłożył własne środki."
        ),
        ("faq_leftover_pot" to AppLanguage.PL) to FaqLocaleText(
            category = "Wspólna kasa",
            question = "Co dzieje się z pieniędzmi pozostałymi we wspólnej kasie po podróży?",
            answer = "W zakładce «Rozliczenia» aplikacja wylicza dokładną kwotę zwrotu dla każdego uczestnika proporcjonalnie do wpłat i rzeczywistej konsumpcji."
        ),
        ("faq_add_expense" to AppLanguage.PL) to FaqLocaleText(
            category = "Wydatki",
            question = "Jak dodać wydatek do podróży?",
            answer = "W zakładce «Wydatki» naciśnij «+». Wprowadź opis, kwotę, datę, kategorię, osobę płacącą i zaznacz uczestników, którzy korzystali z wydatku."
        ),
        ("faq_split_expenses" to AppLanguage.PL) to FaqLocaleText(
            category = "Wydatki",
            question = "Jak podzielić wydatek między uczestników? (Podział wydatków)",
            answer = "Domyślnie wszyscy są zaznaczeni do równego podziału. Odznacz osoby, które nie brały udziału, aby podzielić kwotę tylko między pozostałych."
        ),
        ("faq_order_by_date" to AppLanguage.PL) to FaqLocaleText(
            category = "Wydatki",
            question = "Jak przeglądać lub sortować wydatki według daty?",
            answer = "Wydatki są grupowane i sortowane według dni. Skorzystaj z wyszukiwarki lub filtrów kategorii i uczestników."
        ),
        ("faq_attach_photo" to AppLanguage.PL) to FaqLocaleText(
            category = "Wydatki",
            question = "Jak dołączyć zdjęcie paragonu do wydatku?",
            answer = "Podczas dodawania lub edycji wydatku dotknij ikony aparatu lub galerii, aby zapisać zdjęcie rachunku."
        ),
        ("faq_gps_location" to AppLanguage.PL) to FaqLocaleText(
            category = "Wydatki",
            question = "Jak działa lokalizacja GPS i podpowiedzi miejsc?",
            answer = "Przy włączonej lokalizacji Travel Cash podpowiada pobliskie punkty za pomocą OpenStreetMap i oznacza wydatek w zakładce «Mapa»."
        ),
        ("faq_splits_calculation" to AppLanguage.PL) to FaqLocaleText(
            category = "Rozliczenia i salda",
            question = "Jak aplikacja oblicza długi i salda w «Rozliczeniach»?",
            answer = "Dla każdej osoby oblicza się sumę wpłat (z własnej kieszeni + do kasy) minus sumę konsumpcji. Różnica wskazuje należność (+) lub dług (-)."
        ),
        ("faq_optimized_settlements" to AppLanguage.PL) to FaqLocaleText(
            category = "Rozliczenia i salda",
            question = "Czym są «Zoptymalizowane rozliczenia»?",
            answer = "Algorytm Travel Cash redukuje wszystkie wzajemne długi do minimalnej liczby przelewów bezpośrednich, zamykając wszystkie rozliczenia w kilku płatnościach."
        ),
        ("faq_change_currency" to AppLanguage.PL) to FaqLocaleText(
            category = "Waluty i budżet",
            question = "Jak zmienić walutę lub format waluty?",
            answer = "Walutę bazową ustala się podczas tworzenia podróży. Przy pojedynczych wydatkach można wybrać walutę obcą i kurs wymiany."
        ),
        ("faq_budget_alerts" to AppLanguage.PL) to FaqLocaleText(
            category = "Waluty i budżet",
            question = "Jak działa budżet i pasek postępu wydatków?",
            answer = "Pasek pokazuje procent wykorzystanego budżetu. Po przekroczeniu 90% zmienia kolor na czerwony, ostrzegając przed przekroczeniem limitu."
        ),
        ("faq_export_zip" to AppLanguage.PL) to FaqLocaleText(
            category = "Eksport i kopia zapasowa",
            question = "Jak wyeksportować podróż do pliku ZIP?",
            answer = "W «Ustawieniach» w sekcji «Eksport i import» naciśnij «Eksportuj». Wygenerowany plik ZIP otworzy się w natywnym menu udostępniania Androida."
        ),
        ("faq_export_pdf" to AppLanguage.PL) to FaqLocaleText(
            category = "Eksport i kopia zapasowa",
            question = "Jak wygenerować raport PDF z podróży?",
            answer = "W zakładce «Podróże» naciśnij «Eksportuj do PDF». Travel Cash wygeneruje pełny raport finansowy i otworzy menu udostępniania."
        ),
        ("faq_backup_restore" to AppLanguage.PL) to FaqLocaleText(
            category = "Eksport i kopia zapasowa",
            question = "Jak wykonać kopię zapasową lub przywrócić podróż?",
            answer = "Wyeksportuj plik .zip lub .json na Dysk Google lub pocztę. Na nowym urządzeniu użyj opcji «Importuj», aby przywrócić wszystkie dane."
        ),
        ("faq_share_sheet" to AppLanguage.PL) to FaqLocaleText(
            category = "Eksport i kopia zapasowa",
            question = "Jak udostępniać pliki przez WhatsApp, e-mail lub Dysk Google?",
            answer = "Każdy eksport otwiera bezpośrednio natywne menu udostępniania Androida (Share Sheet), pozwalając wybrać docelową aplikację."
        ),
        ("faq_change_language" to AppLanguage.PL) to FaqLocaleText(
            category = "Ustawienia i personalizacja",
            question = "Jak zmienić język aplikacji?",
            answer = "W zakładce «Ustawienia» wybierz preferowany język z 8 dostępnych (hiszpański, angielski, francuski, włoski, chiński, holenderski, niemiecki, polski)."
        ),
        ("faq_daily_notifications" to AppLanguage.PL) to FaqLocaleText(
            category = "Ustawienia i personalizacja",
            question = "Jak włączyć codzienne przypomnienia o wydatkach?",
            answer = "W «Ustawieniach» włącz przełącznik «Codzienne przypomnienie o wydatkach» i ustaw wybraną godzinę."
        )
    )

    fun getCategory(faqId: String, lang: AppLanguage, defaultEs: String, defaultEn: String): String {
        return when (lang) {
            AppLanguage.ES -> defaultEs
            AppLanguage.EN -> defaultEn
            else -> translations[faqId to lang]?.category ?: defaultEn
        }
    }

    fun getQuestion(faqId: String, lang: AppLanguage, defaultEs: String, defaultEn: String): String {
        return when (lang) {
            AppLanguage.ES -> defaultEs
            AppLanguage.EN -> defaultEn
            else -> translations[faqId to lang]?.question ?: defaultEn
        }
    }

    fun getAnswer(faqId: String, lang: AppLanguage, defaultEs: String, defaultEn: String): String {
        return when (lang) {
            AppLanguage.ES -> defaultEs
            AppLanguage.EN -> defaultEn
            else -> translations[faqId to lang]?.answer ?: defaultEn
        }
    }
}
