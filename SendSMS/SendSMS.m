//
//  SendSMS.m
//  SendSMS
//
//  Created by Trevor Porter on 7/13/16.


#import "SendSMS.h"
#import <React/RCTUtils.h>

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("RCTConvert.h")
#import "RCTConvert.h"
#endif

@implementation SendSMS

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(send:(NSDictionary *)options :(RCTResponseSenderBlock)callback)
{
    _callback = callback;
    if([MFMessageComposeViewController canSendText])
    {
        MFMessageComposeViewController *messageController = [[MFMessageComposeViewController alloc] init];
        NSString *body = options[@"body"];
        NSString *filePath = options[@"attachment"];
        NSArray *recipients = options[@"recipients"];

        if (body) {
          messageController.body = body;
        }

        if (recipients) {
            messageController.recipients = recipients;
        }
        
        if (filePath && MFMessageComposeViewController.canSendAttachments) {
            NSURL *fileURL = [RCTConvert NSURL:filePath];
            if ([MFMessageComposeViewController isSupportedAttachmentUTI:@"public.png"]) {
                [messageController addAttachmentURL:fileURL withAlternateFilename:@"stelladot.png"];
            }
         }

        messageController.messageComposeDelegate = self;
        UIViewController *currentViewController = RCTPresentedViewController();
        dispatch_async(dispatch_get_main_queue(), ^{
            [currentViewController presentViewController:messageController animated:YES completion:nil];
        });
    } else {
        bool completed = NO, cancelled = NO, error = YES;
        _callback(@[@(completed), @(cancelled), @(error)]);
    }
}

-(void) messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result {
    bool completed = NO, cancelled = NO, error = NO;
    switch (result) {
        case MessageComposeResultSent:
            completed = YES;
            break;
        case MessageComposeResultCancelled:
            cancelled = YES;
            break;
        default:
            error = YES;
            break;
    }
    
    _callback(@[@(completed), @(cancelled), @(error)]);

    [controller dismissViewControllerAnimated:YES completion:nil];
}

@end
