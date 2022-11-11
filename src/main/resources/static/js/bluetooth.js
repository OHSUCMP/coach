// see; https://medium.com/going-fullstack/interact-with-bluetooth-devices-using-the-web-bluetooth-api-7984b2509939
const handleCharacteristicValueChanged = (event) => {
    console.log(event.target.value.getUint8(0) + '%');
};

const connectToDeviceAndSubscribeToUpdates = async () => {
    const device = await navigator.bluetooth
        .requestDevice({
            filters: [{ services: ['battery_service']}]
        });
    const server = await device.gatt.connect();
    const service = await server.getPrimaryService('battery_service');
    const characteristic = await service.getCharacteristic('battery_level');
    characteristic.startNotifications();
    characteristic.addEventListener('characteristicvaluechanged', handleCharacteristicValueChanged);
    const reading = await characteristic.readValue();
    console.log(reading.getUint8(0) + '%');
};