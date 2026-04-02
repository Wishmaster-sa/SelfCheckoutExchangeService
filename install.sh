#!/bin/bash

ans=y

if [[ $EUID -ne 0 ]]; then
    echo "Скрипт потрібно запускати з правами root (додайте sudo перед командою)"
    exit 1
fi

clear

sudo apt update

echo "******************************************************************************
*                  встановлення Java JDK 21
******************************************************************************"
if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME     
    _java="$JAVA_HOME/bin/java"
else
    echo "System has no java"
    sudo apt install openjdk-21-jdk -y
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" > "17" ]]; then
        echo version is higher than 17
    else         
        echo version is 17 or less, updating to 21
	sudo apt install openjdk-21-jdk -y
    fi
fi


echo "******************************************************************************
*                  встановлення curl
******************************************************************************"

sudo apt install curl -y

echo "******************************************************************************
*                  встановлення Maven
******************************************************************************"
sudo apt install maven -y

echo "******************************************************************************
*                  встановлення Github client
******************************************************************************"

sudo apt install git -y

echo "******************************************************************************
*                  Клонування SelfCheckoutExchangeService із Github
******************************************************************************"
currentuser=$(stat -c "%G" .)

if [ -e ./SelfCheckoutExchangeService ]; then
	echo "проєкт SelfCheckoutExchangeService вже існує, клонування пропущено"
else 
	git clone https://github.com/Wishmaster-sa/SelfCheckoutExchangeService.git

	sudo chown -R $currentuser:$currentuser ./SelfCheckoutExchangeService
fi


autostartFile="./SelfCheckoutExchangeService/scoex.service" 
sed -i "s/User=sa/User=$currentuser/g" $autostartFile

sudo chown -R $currentuser:$currentuser ./SelfCheckoutExchangeService

cd ./SelfCheckoutExchangeService

echo "******************************************************************************
*                  Компіляція SelfCheckoutExchangeService
******************************************************************************"
/usr/bin/mvn package

sudo chmod +x ./start-scoex.sh


echo "******************************************************************************
*                  ВІТАЄМО! РОЗГОРТАННЯ СЕРЕДОВИЩА ПРОЄКТУ ЗАВЕРШЕНО!
******************************************************************************"

echo "**************************************************************************************
    * Щоб запустити клієнт перейдить в папку проекта (SelfCheckoutExchangeService) 
    * Отредагуйте конфіг файл за допомогою nano ./target/application.yml
    * Вам треба вказати адрес сервіса (server-path)
    * порт клієнта (port)
    * Встановити або змінити файл логування (logging.file.name)
    * Встановити або змінити рівень логування. (NONE - немає логування, INFO - є логування)
    * Встановити налаштування з'єднання з Postgres SQL
    * Зберегите файл налаштувань та виконайте наступну команду:  bash start-scoex.sh
    * Кліент буде доступний за адресою http://localhost:[port]
    ****************************************************************************************"
     

