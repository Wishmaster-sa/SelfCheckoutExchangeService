#!/bin/sh


function installService() {
	echo 'Додаю сервіс до автозапуску...'
 	#modify service daemon for current user
	autostartFile="./scoex.service"
 	currentuser=$(stat -c "%G" .)
	sed -i "s/User=sa/User=$currentuser/g" $autostartFile
	sudo mkdir /opt/SCOExchange
	sudo mkdir /opt/SCOExchange/config
	sudo cp ./target/SelfCheckoutExchangeModule-1.0.0.jar /opt/SCOExchange
        sudo cp -R config/* /opt/SCOExchange/config 
	#sudo cp ./webservice.settings /opt/SCOExchange
	sudo cp ./scoex.service /etc/systemd/system
	sudo systemctl daemon-reload
	sudo systemctl enable scoex.service
	sudo systemctl start scoex.service
	sudo systemctl status scoex.service
}

function removeService() {
	echo "Видаляю Self-checkout exchange сервіс..."
	sudo systemctl stop scoex.service
	sudo systemctl disable scoex.service
	sudo rm /etc/systemd/system/scoex.service
	sudo sudo systemctl daemon-reload
	
	sudo rm -R /opt/SCOExchange
}

function startService() {
	echo "Запускаю Self-checkout exchange сервіс..."
	sudo systemctl start scoex.service
	sudo systemctl status scoex.service
}

function stopService() {
	echo "Зупиняю Self-checkout exchange сервіс..."
	sudo systemctl stop scoex.service
	sudo systemctl status scoex.service
}


if [[ $EUID -ne 0 ]]; then
    echo "Скрипт потрібно запускати з правами root (додайте sudo перед командою)"
    exit 1
fi

if [ -z $1 ]; then
	PS3='Будь-ласка, зробіть вибір: '
	select option in "Додати сервіс до автозапуску" "Запустити сервіс" "Зупинити сервіс" "Видалити сервіс" "Вихід"
	do
	    case $option in
		"Додати сервіс до автозапуску")
		    installService
		    break
		    ;;
		"Запустити сервіс")
		    startService
		    break
		    ;;
		"Зупинити сервіс")
		    stopService
		    break
		    ;;
		"Видалити сервіс")
		    removeService
		    break
		    ;;
		"Вихід")
		    break
		    ;;
		*)
		    echo 'Invalid option.'
		    ;;
	    esac
	done	
	exit 1
fi


case $1 in
    'install')
        installService
        ;;
    'start')
        startService
        ;;
    'stop')
        stopService
        ;;
    'remove')
        removeService
        ;;
    *)
        echo 'Usage: bash springws-service.sh install|start|stop|remove'
        ;;
esac

