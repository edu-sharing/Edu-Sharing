import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatDatepickerInputEvent } from '@angular/material/datepicker';

@Component({
    selector: 'es-share-dialog-choose-date',
    templateUrl: 'choose-date.component.html',
    styleUrls: ['choose-date.component.scss'],
})
export class ShareDialogChooseDateComponent implements OnInit, OnChanges {
    @Input() dateTime: number;
    @Input() from?: number;
    @Input() to?: number;
    @Output() dateTimeChange = new EventEmitter<number>();

    timeControl = new FormControl('', [Validators.pattern(/\d\d:\d\d/)]);
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
        this.timeControl.setValue(
            new DatePipe('en').transform(this.toDate(this.dateTime), 'HH:mm'),
        );
    }

    updateDate(event: MatDatepickerInputEvent<Date, any>) {
        const currentDate = new Date(this.dateTime);
        // keep the hour + minutes so only update the yy-mm-dd
        currentDate.setFullYear(event.value.getFullYear());
        currentDate.setMonth(event.value.getMonth());
        currentDate.setDate(event.value.getDate());
        this.dateTimeChange.emit(currentDate.getTime());
    }
}
