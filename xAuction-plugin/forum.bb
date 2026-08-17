[center][size=7][b][color=#AB47BC]xAuction[/color][/b][/size]
[size=4][color=#CE93D8]Серверные торги[/color][/size]
[size=2][color=#9E9E9E]KL0ND1K3[/color][/size][/center]

[hr]

Сервер выставляет лот. Игроки ставят цену. Кто дал больше и кого не перебили — забирает предмет.

[b][color=#AB47BC]Как это работает[/color][/b]

Раз в 10 минут (время в конфиге) в чат приходит лот: название, стартовая цена, кнопка [b][Участвовать][/b].
Предмет выбирается случайно из списка. Если на сервере никого нет — торг не стартует.

Жмёшь кнопку или пишешь [b]/auction[/b] — открывается меню.
Кликнул по лоту — вводишь сумму [b]выше[/b] текущей ставки (наковальня, чат или команда).
Деньги снимаются сразу. Кого перебили — возвращают.

Не перебили 10 секунд — победа, предмет в инвентарь.
За всё время лота ставок не было — лот сгорает, через паузу идёт следующий.

[b][color=#AB47BC]Функции[/color][/b]

[list]
[*][b][color=#CE93D8]Торги[/color][/b] — лоты по таймеру, анонс в чат с кликом
[*][b][color=#CE93D8]Меню[/color][/b] — лот, ставка, время, баланс, история
[*][b][color=#CE93D8]Ставка[/color][/b] — наковальня, чат или [b]/auction bid 50[/b]
[*][b][color=#CE93D8]Редкие лоты[/color][/b] — пометка, другой анонс и звук
[*][b][color=#CE93D8]Победа за 10 сек.[/color][/b] — не перебили ставку — предмет твой
[*][b][color=#CE93D8]Защита от опечатки[/color][/b] — ставка больше 50% баланса спрашивает подтверждение
[*][b][color=#CE93D8]История[/color][/b] — последние 10 лотов: кто выиграл и за сколько
[*][b][color=#CE93D8]Выдача приза[/color][/b] — в инвентарь, если полный — на землю или при входе
[*][b][color=#CE93D8]Экономика[/color][/b] — Vault (Essentials, CMI и другие) или коины RPG
[/list]

[b][color=#AB47BC]Команды[/color][/b]

[code]
/auction              меню торгов
/auction bid <сумма>  ставка
/auction history      история
/auction yes / no     подтвердить или отменить крупную ставку
/auction reload       перечитать конфиг      (xauction.admin)
/auction next         следующий лот          (xauction.admin)
[/code]
Алиасы: [b]/auc[/b], [b]/xauction[/b]

[b][color=#AB47BC]Права[/color][/b]

[code]
xauction.use     играть в торги (по умолчанию всем)
xauction.admin   reload и next (по умолчанию операторам)
[/code]

[b][color=#AB47BC]Требования[/color][/b]

[list]
[*][b]Paper 26.2[/b]
[*][b]Java 25[/b]
[*]Folia не поддерживается
[/list]

[b]По желанию:[/b]
[list]
[*][b]Vault[/b] / [b]VaultUnlocked[/b] — EssentialsX, CMI, GemsEconomy, PlayerPoints
[*][b]RPG[/b] — свои коины, если Vault нет
[*][b]ViaVersion[/b] — клиенты со старых версий
[/list]
Экономика [b]auto[/b]: сначала Vault, иначе RPG.

Клиенты: меню и клик в чате с [b]1.8+[/b]. Градиенты — с [b]1.16+[/b].

[b][color=#AB47BC]Конфиг[/color][/b]

[code]
duration-seconds          сколько висит лот
bid-lock-seconds          секунд без перебития до победы
between-auctions-seconds  пауза до следующего лота
confirm-percent           с какого % баланса спрашивать подтверждение
skip-if-empty             не крутить торги на пустом сервере
lots                      предметы, количество, старт, шанс, rare
[/code]
Тексты — [b]messages.yml[/b], вид меню — [b]gui.yml[/b].

[center][color=#9E9E9E]KL0ND1K3[/color][/center]
