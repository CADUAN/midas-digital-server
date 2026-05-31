#!/usr/bin/env bash
# Прогон 14 тестовых сценариев Midas Bank Digital через curl (для скриншотов терминала).
# Перед запуском: docker compose up -d postgres && ./gradlew :server:run (в другом окне).
# Запускать на чистой БД (docker compose down -v). Требуется jq (brew install jq).
set -u
H=http://localhost:8080
PHONE="+79990001122"; PIN="1234"
RPHONE="+79990003344"; RPIN="5678"
line(){ printf '\n========== %s ==========\n' "$1"; }

# --- подготовка получателя ---
RTOKEN=$(curl -s -X POST $H/api/v1/auth/register -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Пётр Сидоров\",\"phone\":\"$RPHONE\",\"pin\":\"$RPIN\"}" | jq -r .token)
RCARD=$(curl -s $H/api/v1/cards -H "Authorization: Bearer $RTOKEN" | jq -r '.cards[0].cardNumber')
# QR-нагрузку собираем из числовых реквизитов получателя (без кавычек/пробелов),
# чтобы тело JSON оставалось корректным; для перевода по QR нужны только эти поля.
RREQ=$(curl -s $H/api/v1/wallet/requisites -H "Authorization: Bearer $RTOKEN")
RQR=$(echo "$RREQ" | jq -r '.requisites | "inn=\(.inn)|kpp=\(.kpp)|bik=\(.bik)|account=\(.account)|correspondentAccount=\(.correspondentAccount)|contractNumber=\(.contractNumber)"')

line "Сценарий 1: Регистрация пользователя"
curl -s -i -X POST $H/api/v1/auth/register -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Никита Никонов\",\"phone\":\"$PHONE\",\"pin\":\"$PIN\"}" | head -1
TOKEN=$(curl -s -X POST $H/api/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"phone\":\"$PHONE\",\"pin\":\"$PIN\"}" | jq -r .token)

line "Сценарий 2: Регистрация с занятым номером (ждём 409)"
curl -s -i -X POST $H/api/v1/auth/register -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Никита Никонов\",\"phone\":\"$PHONE\",\"pin\":\"$PIN\"}" | head -1

line "Сценарий 3: Авторизация пользователя"
curl -s -i -X POST $H/api/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"phone\":\"$PHONE\",\"pin\":\"$PIN\"}" | head -1

line "Сценарий 4: Вход с неверным ПИН-кодом (ждём 401)"
curl -s -i -X POST $H/api/v1/auth/login -H 'Content-Type: application/json' \
  -d "{\"phone\":\"$PHONE\",\"pin\":\"9999\"}" | head -1

line "Сценарий 5: Доступ без токена (ждём 401)"
curl -s -i $H/api/v1/accounts | head -1

line "Сценарий 6: Просмотр счетов"
curl -s $H/api/v1/accounts -H "Authorization: Bearer $TOKEN" | jq .

line "Сценарий 7: Просмотр курсов валют"
curl -s $H/api/v1/wallet/rates -H "Authorization: Bearer $TOKEN" | jq .

line "Сценарий 8: Перевод между своими счетами"
curl -s -X POST $H/api/v1/accounts/transfer -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fromType":"CURRENT","toType":"SAVINGS","amount":"100.00","currency":"RUB"}' | jq .

line "Сценарий 9: Конвертация валют"
curl -s -X POST $H/api/v1/wallet/convert -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fromCurrency":"RUB","toCurrency":"USD","amount":"100.00"}' | jq .

line "Сценарий 10: Перевод по номеру телефона"
curl -s -X POST $H/api/v1/transactions/transfer -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"recipientPhone\":\"$RPHONE\",\"amount\":\"50.00\",\"note\":\"тест\"}" | jq .

line "Сценарий 11: Перевод суммы больше баланса (ждём 422)"
curl -s -i -X POST $H/api/v1/transactions/transfer -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"recipientPhone\":\"$RPHONE\",\"amount\":\"99999999.00\"}" | head -1

line "Сценарий 12: Перевод по номеру карты"
curl -s -X POST $H/api/v1/transactions/transfer/card -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"cardNumber\":\"$RCARD\",\"amount\":\"75.00\"}" | jq .

line "Сценарий 13: Перевод по QR-коду"
curl -s -X POST $H/api/v1/transactions/transfer/qr -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "$(jq -nc --arg q "$RQR" '{qrPayload:$q, amount:"25.00"}')" | jq .

line "Сценарий 14a: История операций"
curl -s $H/api/v1/transactions -H "Authorization: Bearer $TOKEN" | jq .
line "Сценарий 14b: Поиск операций (query=2200)"
curl -s "$H/api/v1/transactions/search?query=2200" -H "Authorization: Bearer $TOKEN" | jq .
