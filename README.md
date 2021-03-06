# Smart Receipts

> A Better Receipt Scanner

![SmartReceipts](smartReceiptsLibrary/src/main/res/drawable-xxxhdpi/icon.png)

Turn your phone into a receipt scanner and expense report generator with Smart Receipts! With Smart Receipts, you can track your receipts and easily generate beautiful PDF and CSV reports.
 
Download Smart Receipts on the Google Play Store:
 
 - [Smart Receipts](https://play.google.com/store/apps/details?id=wb.receipts). The free version of the app, but it also supports an in-app purchase subscription.
 - [Smart Receipts Plus](https://play.google.com/store/apps/details?id=wb.receiptspro). A 'paid' app from back before Google supported in-app purchases.

The free and plus versions versions are identical, except the plus version offers the following enhancements:

- The plus version has no ads
- The plus version supports automatic backups (Android only - coming to iOS in the next update)
- The plus version automatically processes exchange rate conversions (with the newest version)
- The plus version allows you to edit/customize the pdf footer 
    
## Table of Contents

- [Features](#features)
- [Install](#install)
- [Contribute](#contribute)
- [License](#license)

## Features
- [X] Create expense report "folders" to categorize your receipts
- [X] Take receipt photos with your camera's phone
- [X] Import existing pictures on your device
- [X] Import PDF receipts 
- [X] Save receipt price, tax, and currency
- [X] Tag receipt names, categories, payment method, comments, and other metadata
- [X] Create/edit/delete all receipt categories
- [X] Track distance traveled for mileage reimbursement
- [X] Automatic exchange rate processing
- [X] Smart prediction based on past receipts
- [X] Generate PDF, CSV, & ZIP reports
- [X] Fully customizable report output
- [X] Automatic backup support via Google Drive
- [ ] OCR support for receipt scans
- [ ] Graphical breakdowns of spending per category

## Install 

Smart Receipts is broken into a few core modules:

- SmartReceiptsFree. An 'application' module, which contains the free version of the app.
- SmartReceiptsPRO. An 'application' module, which contains the plus version of the app.
- SmartReceiptsLibrary. All common code between the free and plus versions of the app. Nearly all development work will occur in here
- WBMiniLibrary. A few legacy items that haven't been moved into the Library module, but it's otherwise unused.

To install, clone or pull down this project. Please note that it will **NOT** work out of the box, so you will need to add the following files to ensure it will compile (if desired):
  
* google-services.json. This needs to be added to both the free and plus favors at the root level in order for Firebase to function. Please [refer to the Firebase documentation](https://firebase.google.com/) for more details:
 * app/src/free/google-services.json
 * app/src/plusFlavor/google-services.json
* app/src/main/res/values/secrets.xml. You can copy the secrets.xml.sample file and rename the keys inside to achieve this behavior. This is used for low usage keys
* app/src/free/res/values/ads.xml. The ads file in smartReceiptsFree. You can add a key here if you wish to enable ads.
* app/src/free/res/xml/analytics.xml. The analytics file in smartReceiptsFree. You can add a key here if you wish to enable Google Analytics.

Generally speaking, it's easier to test against SmartReceiptsPlus (ie Smart Receipts PlusFlavor), since there are less secrets that have been explicitly gitignored.

## Contribute

Contributions are always welcome! Please [open an issue](https://github.com/wbaumann/SmartReceiptsLibrary/issues/new) to report a bug or file a feature request to get started.  

## License
```
The GNU Affero General Public License (AGPL)

Copyright (c) 2012-2017 Smart Receipts LLC (Will Baumann)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```