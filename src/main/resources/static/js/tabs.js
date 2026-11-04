function setupTabs() {
    let tabsContainer = $('#tabs');
    tabsContainer.empty();

    $('.tabData').each(function() {
        let contentId = $(this).attr('id');
        let label = $(this).attr('data-label');
        let selected = ! $(this).hasClass('hidden');

        let tab = $('<div></div>')
            .addClass('tab')
            .attr('data-contentId', contentId)
            .text(label);

        if (selected) tab.addClass('selected');
        tabsContainer.append(tab);
    });

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