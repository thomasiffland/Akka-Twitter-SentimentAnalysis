import {BrowserModule} from '@angular/platform-browser';
import {Injectable, NgModule} from '@angular/core';
import {LeafletModule} from '@asymmetrik/ngx-leaflet';
import {AppComponent} from './app.component';
import {WebsocketServiceService} from "./websocket-service.service";
import {Socket, SocketIoModule, SocketIoConfig} from 'ng-socket-io';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {TagInputModule} from "ngx-chips";
import {ToastModule} from 'ng2-toastr/ng2-toastr';

const config: SocketIoConfig = {url: 'http://localhost:8080', options: {autoConnect: false, reconnection: false}};

import {ToastOptions} from 'ng2-toastr';

@Injectable()
export class CustomOption extends ToastOptions {
  animate = 'flyRight'; // you can override any options available
  positionClass = 'toast-bottom-right';
  maxShown = 3;
}



@Injectable()
export class ConfigurableSocket extends Socket {

  constructor(url: string) {
    super({ url: url, options: {reconnection: false} });
  }

}

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    SocketIoModule.forRoot(config),
    FormsModule,
    ReactiveFormsModule,
    TagInputModule,
    LeafletModule.forRoot(),
    NgbModule.forRoot(),
    ToastModule.forRoot()
  ],
  providers: [WebsocketServiceService,{provide: ToastOptions, useClass: CustomOption}, ConfigurableSocket],
  bootstrap: [AppComponent]
})
export class AppModule {
}
