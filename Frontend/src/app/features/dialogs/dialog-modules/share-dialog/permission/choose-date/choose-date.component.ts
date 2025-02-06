import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatDatepicker, MatDatepickerInputEvent } from '@angular/material/datepicker';
import { MatInput } from '@angular/material/input';
import { Toast, TranslationsService } from 'ngx-edu-sharing-ui';
import { DateAdapter } from '@angular/material/core';

@Component({
    selector: 'es-share-dialog-choose-date',
    templateUrl: 'choose-date.component.html',
    styleUrls: ['choose-date.component.scss'],
})
export class ShareDialogChooseDateComponent implements OnInit, OnChanges {
    @ViewChild(MatDatepicker) matDatepicker: MatDatepicker<any>;
    @ViewChild(MatInput) matInput: MatInput;
    @Input() dateTime: number;
    @Input() from?: number;
    @Input() to?: number;
    @Output() dateTimeChange = new EventEmitter<number>();

    timeControl = new FormControl('', [Validators.pattern(/\d\d:\d\d/)]);
    constructor(
        private toast: Toast,
        private translationsService: TranslationsService,
        private dateAdapter: DateAdapter<any>,
    ) {
        this.dateAdapter.setLocale(this.translationsService.getLocale());
    }
    toDate(value: number) {
        return value ? new Date(value) : null;
    }
    ngOnInit(): void {
        this.timeControl.valueChanges.subscribe((value) => {
            if (this.timeControl.valid) {
                const date = this.toDate(this.dateTime);
                const valueSplit = value.split(':');
                date.setHours(parseInt(valueSplit[0]), parseInt(valueSplit[1]));
                this.dateTime = date.getTime();
                this.dateTimeChange.emit(this.dateTime);
            }
        });
    }
    ngOnChanges(changes: SimpleChanges): void {
        if (this.from) {
            const date = new Date(this.from);
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            date.setMilliseconds(0);
            this.from = date.getTime();
        }
        this.timeControl.setValue(
            new DatePipe('en').transform(this.toDate(this.dateTime), 'HH:mm'),
        );
        setTimeout(() => (this.matInput.value = this.toDate(this.dateTime)));
    }

    updateDate(event: MatDatepickerInputEvent<Date, any>) {
        const currentDate = new Date(this.dateTime);
        if (
            !event.value ||
            (this.from && event.value?.getTime() < this.from) ||
            (this.to && event.value?.getTime() > this.to)
        ) {
            this.toast.error(null, 'WORKSPACE.SHARE.TIMEBASED.INVALID_DATE');
            this.matInput.value = this.toDate(this.dateTime);
            return;
        }
        // keep the hour + minutes so only update the yy-mm-dd
        currentDate.setFullYear(event.value.getFullYear());
        currentDate.setMonth(event.value.getMonth());
        currentDate.setDate(event.value.getDate());
        this.dateTimeChange.emit(currentDate.getTime());
    }
}
