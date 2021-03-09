function setupTabs() {
    let html = "";
    $('.tabData').each(function() {
        let contentId = $(this).attr('id');
        let label = $(this).attr('data-label');
        let selected = ! $(this).hasClass('hidden');

        html += '<div class="tab';
        if (selected) html += ' selected';
        html += '" data-contentId="' + contentId + '">' + label + '</div>\n';
    });

    $('#tabs').html(html);

    $(document).on('click', '.tab', function() {
        let previousTab = $('#tabs > .tab.selected');
        $(previousTab).removeClass('selected');
        let previousContentId = $(previousTab).attr('data-contentId');
        $('#' + previousContentId).addClass('hidden');

        $(this).addClass('selected');
        let contentId = $(this).attr('data-contentId');
        $('#' + contentId).removeClass('hidden');
    });
}


$(document).ready(function() {
    setupTabs();
    enableHover('.tab');
});