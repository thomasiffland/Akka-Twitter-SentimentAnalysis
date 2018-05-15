import {Component, OnInit, ViewContainerRef, ViewEncapsulation} from '@angular/core';
import {latLng, tileLayer} from 'leaflet';
import {WebsocketServiceService} from "./websocket-service.service";
import 'rxjs/add/operator/map';
import {Subject} from "rxjs/Subject";
import {Socket, SocketIoModule, SocketIoConfig} from 'ng-socket-io';
import {Observable} from "rxjs/Observable";
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import {NgbModal, ModalDismissReasons} from '@ng-bootstrap/ng-bootstrap';
import {ToastsManager} from 'ng2-toastr/ng2-toastr';
import {ConfigurableSocket} from './app.module';
import {TagModel} from "ngx-chips/core/accessor";
import 'rxjs/add/observable/of';
import has = Reflect.has;

declare var HeatmapOverlay;

export interface Message {
  author: string,
  message: string,
  newDate?: string
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent implements OnInit {
  title = 'app';

  connected: boolean = false;

  constructor(private staticSocket: Socket, private modalService: NgbModal, public toastr: ToastsManager, vcr: ViewContainerRef) {
    this.toastr.setRootViewContainerRef(vcr);
  }

  private dataSocket: ConfigurableSocket;

  hashtags: string[] = ['trump'];
  langs: string[] = ['en'];
  quanFilters: string[] = ['20'];

  closeResult: string;

  open(content) {
    this.modalService.open(content).result.then((result) => {
      this.closeResult = `Closed with: ${result}`;
    }, (reason) => {
      this.closeResult = `Dismissed ${this.getDismissReason(reason)}`;
    });
  }

  private getDismissReason(reason: any): string {
    if (reason === ModalDismissReasons.ESC) {
      return 'by pressing ESC';
    } else if (reason === ModalDismissReasons.BACKDROP_CLICK) {
      return 'by clicking on a backdrop';
    } else {
      return `with: ${reason}`;
    }
  }

  dataPositive = {
    data: []
  };
  dataNegative = {
    data: []
  };

  private heatmapLayerNegative = new HeatmapOverlay({
    radius: 2,
    maxOpacity: 0.8,
    latField: 'lat',
    lngField: 'lng',
    valueField: 'count',
    scaleRadius: true,
    useLocalExtrema: true,
    gradient: {
      0.2: '#F44336',
      0.4: '#E53935',
      0.6: '#D32F2F',
      0.8: '#C62828',
      1.0: '#B71C1C',
    }
  });

  private heatmapLayerPositive = new HeatmapOverlay({
    radius: 2,
    maxOpacity: 0.8,
    latField: 'lat',
    lngField: 'lng',
    valueField: 'count',
    scaleRadius: true,
    useLocalExtrema: true,
    gradient: {
      0.2: '#4CAF50',
      0.4: '#43A047',
      0.6: '#388E3C',
      0.8: '#2E7D32',
      1.0: '#1B5E20',
    }
  });

  private options = {
    layers: [
      tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 10,
        minZoom: 1,
        noWrap: true,
        attribution: '...'
      })
    ],
    zoom: 3,
    center: latLng(38.296842, 3.118135)
  };
  public messages: Subject<Message> = new Subject<Message>();
  private map: L.Map;


  ngOnInit(): void {

  }

  onMapReady(map: L.Map): void {
    this.map = map;
    this.heatmapLayerNegative.addTo(this.map);
    this.heatmapLayerPositive.addTo(this.map);
    // this.map.addLayer(this.heatmapLayerPositive);
    //this.map.addLayer(this.heatmapLayerNegative);
  }

  connect() {
    this.connected = true;
    this.start();
  }

  disconnect() {
    this.connected = false;
    this.reset()
    this.staticSocket.disconnect();
    this.dataSocket.disconnect();
  }

  start() {
    this.staticSocket.removeAllListeners("connectionRequestAccepted")
    this.staticSocket.removeAllListeners("disconnect")


    this.staticSocket.on("connectionRequestAccepted", (clientPort) => {
      console.log('Client has connected to the server on connectionRequest socket.');
      this.dataSocket = new ConfigurableSocket('http://127.0.0.1:' + clientPort)

      let data: string[][] = [this.hashtags, this.langs, this.quanFilters];
      this.dataSocket.emit("changeFilters", data);

      this.dataSocket.on('connect', () => {
        console.log('Client has connected to the server on exclusive socket.');
      });

      this.dataSocket.on('tweet', (message, location, emotion) => {
         console.log('Received a message from the server: ' + message + " / " + emotion);
        this.handleTweet(message, location, emotion)
      });

      this.dataSocket.on('disconnect', () => {
        console.log('The client has disconnected due to server command from exclusive socket.');
        this.dataSocket.disconnect(0);
        this.dataSocket.ioSocket.disconnect()
        delete(this.dataSocket);
      });
    });

    this.staticSocket.on('disconnect', () => {
      console.log('Client was disconnected from server on connectionRequest socket.');
      this.staticSocket.disconnect()
    });

    this.staticSocket.connect();
  }


  private toPlainStrings(hashtags: string[]) {
    let returnArray = [];
    for (let h of hashtags) {
      if (typeof h === 'object') {
        returnArray.push(h['value'])
      } else {
        returnArray.push(h);
      }
    }
    return returnArray;
  }

  settingsChanged() {
    this.hashtags = this.toPlainStrings(this.hashtags);
    this.langs = this.toPlainStrings(this.langs);
    this.quanFilters = this.toPlainStrings(this.quanFilters);

    if (this.connected && this.hashtags.length > 0 && this.langs.length > 0 && this.quanFilters.length > 0) {
      console.log(this.hashtags);
      console.log(this.langs);
      console.log(this.quanFilters);

      let data: string[][] = [this.hashtags, this.langs, this.quanFilters];
      this.dataSocket.emit("changeFilters", data);
      this.reset()
    }
  }

  private counter = 0;

  private handleTweet(message, location, emotion) {
    let parsedEmotion = JSON.parse(emotion);
    let success = parsedEmotion[0] == "positive";
    let jsonLocation = JSON.parse(location);
    if (!(Object.keys(jsonLocation).length === 0)) {
      let lon = jsonLocation[0]["lon"];
      let lat = jsonLocation[0]["lat"];
      let confidence = parsedEmotion[1] as number
      if (success) {
        this.toastr.success('<span style="font-size: xx-small">' + message + '</span>', "Confidence: " + Math.round(confidence * 100) / 100 + "%", {enableHTML: true});
        this.counter = 0;
        this.dataPositive.data.push({lat: lat, lng: lon, count: 20})
        this.heatmapLayerPositive.setData(this.dataPositive);

      } else {
        this.toastr.error('<span style="font-size: xx-small">' + message + '</span>', "Confidence: " + Math.round(confidence * 100) / 100+ "%", {enableHTML: true});
        this.counter = 0;
        this.dataNegative.data.push({lat: lat, lng: lon, count: 20})
        this.heatmapLayerNegative.setData(this.dataNegative);

      }
    }

  }

  reset() {
    this.dataPositive.data = [];
    this.dataNegative.data = [];
    this.heatmapLayerPositive.setData(this.dataPositive);
    this.heatmapLayerNegative.setData(this.dataNegative);
  }
}
